from flask import Flask, Blueprint, current_app
from flask import request
from alpha_vantage.timeseries import TimeSeries
from shared import mysql
import json
import sys
import traceback

prices = Blueprint('prices', __name__, template_folder='templates')

#return price history of product with specified ticker
@prices.route('/price/view')
def getPriceHistory():
    sym = request.args.get("sym", default = None, type = str)
    ts = TimeSeries(key='redacted')
    # Get json object with the intraday data and another with  the call's metadata
    try:
        data, meta_data = ts.get_monthly_adjusted(sym)
    except Exception as ex:
        res = current_app.make_response("Could not fetch price data")
        print(traceback.format_exc())
        res.status_code = 404
        return res
    prices = {}
    for k in data.keys():
        #pick closing price for each day
        price =  data[k]["4. close"]
        #ignore gaps
        if(float(price) != 0):
            prices[k] = data[k]["4. close"]
    #todo store prices in new db table?
    return json.dumps(prices)
