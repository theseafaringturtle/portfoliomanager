from flask import Flask
from flask import request
from flask import session
from port import portfolios
from prices import prices
from shared import mysql
import json
from email.utils import parseaddr
import sys
import hashlib
import base64
from datetime import timedelta

app = Flask(__name__)
app.config["MYSQL_DB"] = "redacted"
#Redacted
mysql.init_app(app)
app.register_blueprint(portfolios)
app.register_blueprint(prices)
#sys.stdout = sys.stderr = open('log.txt','wt')

app.secret_key = "redacted"
DEBUG_NOLOGIN = True
#if len(sys.argv)> 1 and sys.argv[1] == "nologin":
#    DEBUG_NOLOGIN = True

@app.before_request
def before_request():
    if DEBUG_NOLOGIN:
        return
    if "token" not in session and request.endpoint != "loginUser":
        res = app.make_response("You are not logged in")
        res.status_code = 403
        return res
    session.permanent = True
    app.permanent_session_lifetime = timedelta(minutes=20)
    session.modified = True

@app.route('/login', methods=['POST'])
def loginUser():
    if "user" not in request.form or "pass" not in request.form:
        return "Fields must not be empty"
    #Redacted
    if "token" not in session:
        return "Invalid username or password"
    return "Already logged in"

@app.route('/client/list')
def listClients():
    cur = mysql.connection.cursor()
    cur.execute('''SELECT Id, FirstName, LastName FROM client''')
    rv = cur.fetchall()
    return json.dumps(rv)

@app.route('/client/view')
def viewClient():
    id = request.args.get("id", default = None, type = str)
    if not id:
        res = app.make_response("Id is not valid")
        res.status_code = 400
        return res
    cur = mysql.connection.cursor()
    #retrieve contact details for client
    cur.execute('''SELECT * FROM client WHERE Id={}'''.format(id))
    contactDetails = cur.fetchall()
    if len(contactDetails) > 0:#details list is not empty
        contactDetails = contactDetails[0]
    return json.dumps(contactDetails)

@app.route('/client/update', methods = ['POST'])
def updateClient():
    id = request.args.get("id", default = None, type = str)
    if not id:
        res = app.make_response("Id is not valid")
        res.status_code = 400
        return res
    #chain arguments together in UPDATE...SET statement
    updateStr = "" 
    firstName = request.args.get("fname", default = None, type = str)
    lastName = request.args.get("lname", default = None, type = str)
    phone = request.args.get("phone", default = None, type = str)
    email = request.args.get("email", default = None, type = str)
    valid, errorRes = checkClientInfo(False,firstName,lastName,phone,email)
    if not valid:
        return errorRes
    if firstName:
        updateStr += "FirstName='"+firstName+"',"
    if lastName:
        updateStr += "LastName='"+lastName+"',"
    if phone:
        updateStr += "PhoneNumber='"+phone+"',"
    if email:
        updateStr += "Email='"+email+"',"
    #end of query string
    if updateStr[-1] == ',':
        updateStr = updateStr[:-1]
    cur = mysql.connection.cursor()
    query = '''UPDATE client SET {} WHERE Id={} '''.format(updateStr,id)
    print(query,file=sys.stderr)
    cur.execute(query)
    mysql.connection.commit()
    res = app.make_response("Client updated successfully")
    res.status_code = 200
    return res#todo catch _mysql_exceptions.ProgrammingError:

@app.route('/client/add', methods = ['POST'])
def addClient():
    firstName = request.args.get("fname", default = None, type = str)
    lastName = request.args.get("lname", default = None, type = str)
    phone = request.args.get("phone", default = None, type = str)
    email = request.args.get("email", default = None, type = str)
    valid, errorRes = checkClientInfo(True,firstName,lastName,phone,email)
    if not valid:
        return errorRes
    cur=mysql.connection.cursor()
    query = '''INSERT INTO client(Id,FirstName,LastName,PhoneNumber,Email)
                    VALUES(NULL,'{}','{}','{}','{}' )'''.format(firstName,lastName,phone,email)
    print(query,file=sys.stderr)
    cur.execute(query)
    mysql.connection.commit()
    res = app.make_response("New client added successfully")
    res.status_code = 200
    return res
    
#if we're not checking every single field, allow None to be passed as parameter
def checkClientInfo(completeCheck,firstName, lastName, phone, email):
    res = None
    if completeCheck and None in [firstName, lastName, phone, email]:
        res = app.make_response("Fields cannot be left empty")
    elif firstName and len(firstName) > 64:
        res = app.make_response("First name is not valid")
    elif lastName and len(lastName) > 64:
        res = app.make_response("Last name is not valid")
    elif phone and (len(phone) > 32 or not phone.isdigit()):
        res = app.make_response("Phone number is not valid")
    elif email and (len(email) > 64 or "@" not in parseaddr(email)[1]):
        res = app.make_response("Email is not valid")
    if res:
        res.status_code = 400
        print(str(res.get_data(),"utf-8"))#reason will be logged to file
        return False,res
    else:
        return True, None

@app.route('/')
def useless():
    return "derp"

#print(app.url_map)

if __name__ == '__main__':
    app.run(debug=True)
