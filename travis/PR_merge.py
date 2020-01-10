import json
import sys
import os
import requests
import datetime

DISCORD_ENDPOINT = sys.argv[1]
TRAVIS_BUILD_NUMBER = sys.argv[2]
TRAVIS_BUILD_WEB_URL = sys.argv[3]
DISCORD_ENDPOINT_2 = sys.argv[4]

timestamp = datetime.datetime.now().strftime("%Y_%m_%d__%H_%M_%S")
filename = "WormJam"+timestamp+".tar.gz"

files = {'results.json': open('results.json', 'rb')}
files2 = {filename:open("WormJam.tar.gz",'rb')}
payload_json = {
    "embeds": [{
        "title": "WormJam CI Report",
        "color": 16709211,
        "description": "PR Merge",
        "fields":[
            {
                "name": "Build Number",
                "value":str(TRAVIS_BUILD_NUMBER)
            },
            {
                "name":"Build logs",
                "value":"Logs can be found [here]("+TRAVIS_BUILD_WEB_URL+")"
            }
        ],
        "thumbnail": {
            "url": "https://travis-ci.com/images/logos/Tessa-1.png"
        },
        "timestamp": str(datetime.datetime.now().isoformat())
    }]
}

r =requests.post(DISCORD_ENDPOINT,data=json.dumps(payload_json), headers={"Content-Type": "application/json"})
<<<<<<< HEAD
r2 = requests.post(DISCORD_ENDPOINT, files=files)
=======
r2 = requests.post(DISCORD_ENDPOINT, files=files)
print(r,r2)
r3 =requests.post(DISCORD_ENDPOINT_2,data=json.dumps(payload_json), headers={"Content-Type": "application/json"})
r4 = requests.post(DISCORD_ENDPOINT_2, files=files2)
print(r3,r4)
>>>>>>> devel
