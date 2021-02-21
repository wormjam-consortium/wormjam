import json
import sys
import os
import requests
import datetime


DISCORD_ENDPOINT = sys.argv[1]
DISCORD_ENDPOINT_2 = sys.argv[2]
GITHUB_BUILD_NUMBER = sys.argv[3]  # Github counter
GITHUB_BUILD_WEB_URL = sys.argv[4]  # github unique run ID used for link construction
GITHUB_REPO_SLUG = sys.argv[5]  # user/repo

timestamp = datetime.datetime.now().strftime("%Y_%m_%d__%H_%M_%S")
filename = "WormJam" + timestamp + ".tar.gz"

files = {"Report.html": open("Report.html", "rb")}
files2 = {filename: open("WormJam.tar.gz", "rb")}
payload_json = {
    "embeds": [
        {
            "title": "WormJam CI Report",
            "color": 16709211,
            "description": "Model Build from [%s](%s)"
            % (GITHUB_REPO_SLUG, "https://github.com/" + GITHUB_REPO_SLUG),
            "fields": [
                {"name": "Build Number", "value": str(GITHUB_BUILD_NUMBER)},
                {
                    "name": "Build logs",
                    "value": "Logs can be found [here](https://github.com/%s/actions/runs/%s)"
                    % (GITHUB_REPO_SLUG, GITHUB_BUILD_WEB_URL),
                },
            ],
            "thumbnail": {
                "url": "https://avatars1.githubusercontent.com/u/44036562?s=280&v=4"
            },
            "timestamp": str(datetime.datetime.now().isoformat()),
        }
    ]
}

r = requests.post(
    DISCORD_ENDPOINT,
    data=json.dumps(payload_json),
    headers={"Content-Type": "application/json"},
)
r2 = requests.post(DISCORD_ENDPOINT, files=files)
print(r, r2)
r3 = requests.post(
    DISCORD_ENDPOINT_2,
    data=json.dumps(payload_json),
    headers={"Content-Type": "application/json"},
)
r4 = requests.post(DISCORD_ENDPOINT_2, files=files2)
print(r3, r4)
