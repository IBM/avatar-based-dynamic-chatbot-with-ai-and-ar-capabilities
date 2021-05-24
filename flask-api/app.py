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

    getRestaurantsFromLatAndLon = 'https://penqb9t3esyypld1.maps.arcgis.com/apps/instant/3dviewer/index.html?appid=93519dc612e94a67bd62d5649e6c98d6'
    header = {"User-agent": "curl/7.43.0", "Accept": "application/json", "user-key": "8eaf379988ed4e1eb1026079832b16ce"}
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
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="initial-scale=1, maximum-scale=1, user-scalable=no">
<title>ArcGIS Developer Guide: Forward geocoding</title>
    <!-- ArcGIS Mapping APIs and Location Services Developer Guide
    Learn more: https://developers.arcgis.com/documentation/mapping-apis-and-services/search/
    -->
<style>
  html, body, #viewDiv {
    padding: 0;
    margin: 0;
    height: 100%;
    width: 100%;
  }
</style>

<link rel="stylesheet" href="https://js.arcgis.com/4.19/esri/themes/light/main.css">
<script src="https://js.arcgis.com/4.19/"></script>

<script>
  require([
    "esri/config",
    "esri/Map",
    "esri/views/MapView",
    "esri/Graphic",
    "esri/tasks/Locator"
  ],(esriConfig, Map, MapView, Graphic, LocatorTask)=> {

    esriConfig.apiKey = "YOUR_API_KEY";

    const map = new Map({
      basemap: "arcgis-light-gray" //Basemap layer service
    });

    const view = new MapView({
      container: "viewDiv",
      map: map,
      constraints: {
        snapToZoom: false
      }
    });

    view.popup.actions = [];

    view.when(()=>{

      const locatorTask = new LocatorTask({
        url: "https://geocode-api.arcgis.com/arcgis/rest/services/World/GeocodeServer"
      });

      const params = {
        address: {
          "address": "1600 Pennsylvania Ave NW, DC"
        }
      }

      locatorTask.addressToLocations(params).then((results) => {
        showResult(results);
      });


      function showResult(results) {
        if (results.length) {
          const result = results[0];
          view.graphics.add(new Graphic({
              symbol: {
                type: "simple-marker",
                color: "#000000",
                size: "8px",
                outline: {
                  color: "#ffffff",
                  width: "1px"
                }
              },
              geometry: result.location,
              attributes: {
                title: "Address",
                address: result.address,
                score: result.score
              },
              popupTemplate: {
                title: "{title}",
                content: "{address}" + "<br><br>" + result.location.longitude.toFixed(5) + "," + result.location.latitude.toFixed(5)
              }
            }
          ));
          view.popup.open({
            features: view.graphics,
            location: result.location
          });
          view.goTo({
            target: result.location,
            zoom: 13
          });
        }
      }
    });
  });
</script>
</head>
<body>
  <div id="viewDiv"></div>
</body>
</html>
