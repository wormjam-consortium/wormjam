import json
import sys
import os
import requests
import datetime

DISCORD_ENDPOINT = sys.argv[1]
BUILD_NUMBER = sys.argv[2]
TRAVIS_BUILD_WEB_URL = sys.argv[3]


files = {'results.json': open('results.json', 'rb')}
payload_json = {
    "embeds": [{
        "title": "WormJam CI Report",
        "color": 16709211,
        "description": "Pull Request",
        "fields":[
            {
                "name": "Build Number",
                "value":str(BUILD_NUMBER)
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
r2 = requests.post(DISCORD_ENDPOINT, files=files)