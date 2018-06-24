from flask import Flask, Blueprint, current_app
from flask import request
from shared import mysql
import json
import sys
import xlrd
import base64
import traceback
from datetime import datetime

portfolios = Blueprint('portfolios', __name__, template_folder='templates')


class Tx():
    def __init__(self,ISIN,qty,oldp,ratio):
        self.ISIN = ISIN
        self.quantity = qty
        self.oldPrice = oldp
        self.ratio = ratio

class Holding():
    def __init__(self,ISIN):
        self.ISIN = ISIN
        self.transactions = []
        self.endValue = 0
        self.ret = 0
        self.currency = ""
        self.description = ""
        self.ticker = ""

#calculate value of each product based on transactions history
@portfolios.route('/port/view')
def showPortfolioContents():
    portId = request.args.get("pid", default = None, type = int)
    if not portId:
        return errorRes("Portfolio id is not valid",400)
    cur = mysql.connection.cursor()
    cur.execute('''SELECT product.ISIN,tradedate,sign,quantity,transaction.price,product.currency,
fxrate, commissions,security,product.description,ticker
        FROM transaction,product
        WHERE transaction.portId = {} and product.ISIN = transaction.ISIN
        ORDER BY ISIN,tradedate'''.format(portId))
    #todo ISIN foreign key constraint assume we have all products in the world in our DB?
    txList = cur.fetchall()
    #python dictionaries with ISIN keys and Holding values
    holdings = {}
    # date of first transaction for each holding
    startDate = 0
    # Transactions are ordered by ISIN and processed sequentially, so
    # this variable signals the end of a block of transactions for a specific ISIN
    lastISIN = ""
    for tx in txList:
        ISIN = tx[0]
        txDate = tx[1]
        sign = tx[2]
        quantity = tx[3]
        oldPrice = tx[4]
        if sign == "Sell":
            quantity = -quantity 
        if lastISIN == ISIN:
            ratio = weightRatio(txDate,startDate,datetime.today().date())
            holdings[ISIN].transactions.append( Tx(ISIN,quantity,oldPrice,ratio) )
        else:
            holdings[ISIN] = Holding(ISIN)
            holdings[ISIN].description = tx[9]
            holdings[ISIN].ticker = tx[10]
            holdings[ISIN].currency = tx[5]
            startDate = txDate
            ratio = weightRatio(txDate,startDate,datetime.today().date())
            holdings[ISIN].transactions.append( Tx(ISIN,quantity,oldPrice,ratio) )
            lastISIN = ISIN
    
    for holding in holdings.values():
        cur.execute('''SELECT price,currency,description FROM product WHERE ISIN="{}"'''.format(holding.ISIN))
        product = cur.fetchone()
        price = product[0]
        currentQuantity = 0
        for tx in holding.transactions:
            currentQuantity += tx.quantity
        #currentValue = currentQuantity * latest price rounded to 2 decimal places
        #assume GBP for now factor in currency later
        holding.endValue = round(currentQuantity * price,2)
    
    for h in holdings.values():
        h.ret = modDietz(h)
    display = []
    for h in holdings.values():
        display.append([h.ISIN,h.description,h.currency,h.endValue,h.ret,h.ticker])
    return json.dumps(display)

# Calculate a weight ratio for each transaction based on date. Recent transactions weigh less on the final score
def weightRatio(txDate,startDate,endDate):
    txDays = (txDate - startDate).days
    totalDays = (endDate - startDate).days
    return (totalDays - txDays)/totalDays

# Implement Modified Dietz formula to evaluate return of investment of a holding
def modDietz(holding):
    numerator = holding.endValue
    denominator = 0
    for t in holding.transactions:
        numerator -= t.quantity * t.oldPrice
        denominator += t.oldPrice * t.ratio
    return round(numerator / denominator,2)
# NOTE: start value is treated as part of the cash flow with weight ratio 1.0

# Show list of portfolios for a client
@portfolios.route('/port/list')
def listPortfoliosForClient():
    id = request.args.get("cid", default = None, type = int)
    if not id:
        return errorRes("Client id is not valid",400)
    cur = mysql.connection.cursor()
    cur.execute('''SELECT id,Description FROM portfolio WHERE clientId={}'''.format(id))
    portList = cur.fetchall()
    return json.dumps(portList)


# User is trying to upload an Excel portfolio. Decode base64 string to file and parse it
@portfolios.route('/port/upload', methods=["POST"])
def receiveExcel():
    cid = request.args.get("cid", default = None, type = int)
    if not cid:
        return errorRes("Client id is not valid",400)
    description = request.form["desc"]
    if not description:
        return errorRes("Description is not valid",400)
    excel = request.form["excel"]
    if not excel:
        return errorRes("Excel file is not valid",400)
    file = base64.b64decode(excel)
    
    portId = createNewPortfolio(cid,description)
    return parseNewPortfolio(file,portId)

# Create new empty portfolio, return new id
def createNewPortfolio(clientId,desc):
    cur = mysql.connection.cursor()
    s ='''INSERT INTO portfolio(id,clientId,description)
                   VALUES(NULL,{},'{}' )'''.format(clientId,desc)
    print(s,file=sys.stdout)
    cur.execute(s)
    cur.execute('''SELECT LAST_INSERT_ID()''')
    #skipped error checking
    return cur.fetchone()[0]

# Parse excel file 
def parseNewPortfolio(file,pid):
    #use library to open excel file
    workbook = xlrd.open_workbook(file_contents=file)
    sheet = workbook.sheet_by_index(0)
    for rowx in range(1,sheet.nrows):
        cols = sheet.row_values(rowx)
        try:# excel internal format int date
            cols[0] = int(cols[0])
            cols[0] = xlrd.xldate.xldate_as_datetime(cols[0],workbook.datemode)
        except ValueError:#date already formatted
            cols[0] = datetime.strptime(cols[0],"%d/%m/%Y")
        dateStr = "{}-{}-{}".format(cols[0].year,cols[0].month,cols[0].day)
        #basic validation checks
        isValid,reason = validateFields(cols[0],cols[1],cols[4],cols[6])
        if not isValid:
            return reason + " - Error at line "+ str(rowx)
        #add rows to db
        cur = mysql.connection.cursor()
        queryStr = '''INSERT INTO `transaction`(`id`, `portId`, `TradeDate`, `Sign`,
                `Security`, `ISIN`, `Quantity`, `Currency`, `Price`, `FxRate`, `Commissions`)
                VALUES( NULL,{},'{}','{}','{}','{}',{},'{}',{},{},{} )
                '''.format(pid,dateStr,cols[1],cols[2],cols[3],cols[4],cols[5],cols[6],cols[7],cols[8])
        print(queryStr,file=sys.stdout)
        #commit createnewportfolio query and parsenewportfolio
        try:
            cur.execute(queryStr)
            mysql.connection.commit()
        except Exception:
            print("Portfolio parsing failed with error:",file=sys.stdout)
            print(traceback.format_exc(),file=sys.stdout)
            return "Portfolio parsing failed"
    return "Portfolio uploaded succesfully"

def validateFields(date,sign,quantity,price):
    if date > datetime.today():
        return (False,"Dates into the future are not allowed")
    elif sign.lower() not in ["sell","buy"]:#hurr durr
        return (False,"Operation not recognised: either buy or sell")
    value = quantity * price
    if value < 0 or value > 10**7:
        return (False,"Total value of transaction out of range")
    return True,"Success"
    
def errorRes(message,code):
    res = current_app.make_response(message)
    res.status_code = code
    return res
