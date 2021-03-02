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
#Developer notes: 
#https://developer.here.com/documentation/geocoder/dev_guide/topics/resource-type-response-geocode.html
#https://developer.here.com/documentation/geocoder/dev_guide/topics/example-location-search-landmark.html
#https://developer.here.com/documentation/geocoder/dev_guide/topics/resource-reverse-geocode.html
#https://developer.here.com/documentation/authentication/dev_guide/topics/api-key-credentials.html
def get_here_restaurants(latitude, longitude):
    listofRestaurantsnearby = []
    api_key = '#use_key_from_bitwarden.com_or_generate_new_key'
    #&radius=500&sort=real_distance&order=asc&start=0
    getRestaurantsFromLatAndLon = 'https://reverse.geocoder.ls.hereapi.com/6.2/reversegeocode.json?mode=retrieveLandmarks&gen=9&prox='+latitude+','+longitude+',500&maxresults=20&apiKey='+api_key
    header = {"User-agent": "curl/7.43.0", "Accept": "application/json"}
    response = requests.get(getRestaurantsFromLatAndLon, headers=header)
    restaurant_info = response.json()["Response"]

    for key,value in restaurant_info.items():
        if(key=='View'):            
            for item in restaurant_info[key][0]["Result"]:
                listofRestaurantsnearby.append(item["Location"]["Name"])
    listofRestaurantsnearbyjson = {"nearby": [x for x in listofRestaurantsnearby]}
    return jsonify(listofRestaurantsnearbyjson)
    #return restaurant_info
   

@app.route("/getlocation", methods=["GET"])
def getlocation():
    lat = request.args['lat']
    lon = request.args['long']
    location = str(lat)+","+str(lon)
    geolocator = Nominatim(user_agent="smart_avatar_application")
    location = geolocator.reverse(location)
    place = location.address
    outputjson = {"place": place.split(',')[0]}

    return jsonify(outputjson)

@app.route("/getrestaurants", methods=["GET"])
def getrestaurants():
    latitude = request.args['lat']
    longitude = request.args['long']
    return get_here_restaurants(latitude, longitude)

@app.route("/")
def main():
    return "<h1>OK</h1>"

port = os.getenv('VCAP_APP_PORT', '8080')
if __name__ == "__main__":
    app.run(debug=True, host='127.0.0.1', port=port)
