import json
import sys
import requests
import datetime


DISCORD_ENDPOINT = sys.argv[1] #Github Actions Channel Webhook
DISCORD_ENDPOINT_2 = sys.argv[2] # Model Uploads Channel Webhook
GITHUB_BUILD_NUMBER = sys.argv[3]  # Github build counter
GITHUB_BUILD_WEB_URL = sys.argv[4]  # github unique run ID used for link construction
GITHUB_REPO_SLUG = sys.argv[5]  # user/repo
GITHUB_REPO_BRANCH = sys.argv[6].split("/")[-1] #branch - process the string and grab the last term

timestamp = datetime.datetime.now().strftime("%Y_%m_%d__%H_%M_%S")
filename = "WormJam" + timestamp + ".tar.gz"

#prepare files for sending
report_file = {"Report.html": open("Report.html", "rb")}
packaged_model_file = {filename: open("WormJam.tar.gz", "rb")}

#construct embed to send to discord
#This embed is used for both messages
payload_json = {
    "embeds": [
        {
            "title": "WormJam CI Report",
            "color": 2132223, #this is github action colour
            "description": "Model Build from [%s](%s) on branch %s"
            % (GITHUB_REPO_SLUG, "https://github.com/" + GITHUB_REPO_SLUG,GITHUB_REPO_BRANCH),
            "fields": [
                {"name": "Build Number", "value": str(GITHUB_BUILD_NUMBER)},
                {
                    "name": "Build logs",
                    "value": "Logs can be found [here](https://github.com/%s/actions/runs/%s)"
                    % (GITHUB_REPO_SLUG, GITHUB_BUILD_WEB_URL),
                },
            ],
            "thumbnail": {
                "url": "https://avatars1.githubusercontent.com/u/44036562?s=280&v=4" #github actions logo
            },
            "timestamp": str(datetime.datetime.now().isoformat()),
        }
    ]
}
#Send the report
r = requests.post(
    DISCORD_ENDPOINT,
    data=json.dumps(payload_json),
    headers={"Content-Type": "application/json"},
)
r2 = requests.post(DISCORD_ENDPOINT, files=report_file)
print(r, r2)
#send the model
r3 = requests.post(
    DISCORD_ENDPOINT_2,
    data=json.dumps(payload_json),
    headers={"Content-Type": "application/json"},
)
r4 = requests.post(DISCORD_ENDPOINT_2, files=packaged_model_file)
print(r3, r4)
