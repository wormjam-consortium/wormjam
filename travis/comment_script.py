import json
import sys
import os
import requests
import datetime

API_KEY = sys.argv[1]
REPO_SLUG = sys.argv[2]
PULL_REQUEST = sys.argv[3]
DISCORD_ENDPOINT = sys.argv[4]
BUILD_NUMBER = sys.argv[5]
TRAVIS_BUILD_WEB_URL = sys.argv[6]
API_ENDPOINT = "https://api.github.com/repos/%s/issues/%s/comments"%(REPO_SLUG,PULL_REQUEST)

# print(API_KEY)
# print(REPO_SLUG)
# print(PULL_REQUEST)
# print(API_ENDPOINT)

headers = {'Authorization':'token '+API_KEY}

def post_to_github(data):
    print("Beginning post to GitHub")
    ddata = {"body":data}
    json_data = json.dumps(ddata)
    comment = requests.post(API_ENDPOINT,headers=headers,data=json_data) 
    if comment.status_code == 201:
        print("API Successful: "+data.split("\n")[0])

with open("results.json","r") as f:
    data = json.loads(f.read())
print("Data loaded")
msg = "## Results"
for key,val in data.get("tests").items():
    if type(val.get("result")) == str:
        msg += "\n### "+key+":\n"+val.get("result")
    else:
        msg += "\n### "+key+":"
        results = val.get("result",{"status":"ERRORED"})
        for key2,val2 in results.items():
            msg += "\n**"+key2 + "**: " + val2
            

post_to_github(msg)

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
        "timestamp": str(datetime.datetime.now().isoformat())
    }]
}

r =requests.post(DISCORD_ENDPOINT,data=json.dumps(payload_json), headers={"Content-Type": "application/json"})
r2 = requests.post(DISCORD_ENDPOINT, files=files)