import json
import requests
import pprint
import reverse_geocoder as rg
from flask_cors import CORS
import pandas as pd
import os
from flask import Flask, jsonify, json, request
from geopy.geocoders import Nominatim

app = Flask(__name__)
app.config['JSON_SORT_KEYS'] = False
CORS(app)

global entity_type,entity_id

@app.route("/reverse", methods=["GET"])
def reverseGeocode(coordinates): 
    result = rg.search(coordinates) 
    location = ""  
    # result is a list containing ordered dictionary.
    for i in result:
        for key, value in i.items():
            if(key=='name'):
                location = value
    return location

def getRestaurants(coordinates):
    lat = coordinates[0]
    lon = coordinates[1]
    listofRestaurantsnearby = []
    information_about_restaurant = []

    getRestaurantsFromLatAndLon = 'https://developers.zomato.com/api/v2.1/search?lat='+str(lat)+'&lon='+str(lon)+ '&radius=500&sort=real_distance&order=asc&start=0&count=20'
    header = {"User-agent": "curl/7.43.0", "Accept": "application/json", "user-key": ""}
    response = requests.get(getRestaurantsFromLatAndLon, headers=header)
    Restaurant_info = response.json()
   
    for k,v in Restaurant_info.items():
        if(k=='restaurants'):
            for i in Restaurant_info[k]:
                information_about_restaurant= i['restaurant'].keys()
                listofRestaurantsnearby.append(i['restaurant']['name'])
    listofRestaurantsnearbyjson = {"nearby": [x for x in listofRestaurantsnearby]}
    return jsonify(listofRestaurantsnearbyjson)

@app.route("/getlocation", methods=["GET"])
def getlocation():
    lat = request.args['lat']
    lon = request.args['lon']
    location = str(lat)+","+str(lon)
    geolocator = Nominatim(user_agent="smart_avatar_application")
    location = geolocator.reverse(location)
    place = location.address
    outputjson = {"place": place.split(',')[0]}
    
    return jsonify(outputjson)

@app.route("/getrestaurants", methods=["GET"])
def getrestaurants():
    lat = request.args['lat']
    lon = request.args['lon']
    coordinates =(lat, lon) 
    return getRestaurants(coordinates)
    
@app.route("/")
def main():
    return "<h1>OK</h1>"

port = os.getenv('VCAP_APP_PORT', '8080')
if __name__ == "__main__":
    app.run(debug=True, host='0.0.0.0', port=port)
