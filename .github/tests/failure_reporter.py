import requests
import json
import sys
import datetime

DISCORD_ENDPOINT = sys.argv[1] #Github Actions Channel Webhook
GITHUB_BUILD_NUMBER = sys.argv[2] # Github build counter
GITHUB_BUILD_WEB_URL = sys.argv[3] # github unique run ID used for link construction
GITHUB_REPO_SLUG = sys.argv[4] # user/repo
GITHUB_REPO_BRANCH = sys.argv[5].split("/")[-1] #branch - process the string and grab the last term

payload_json = {
    "embeds": [
        {
            "title": "WormJam CI Report",
            "color": 10027008, #red
            "description": "A build has failed from [%s](%s) on branch %s"
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
                "url":"https://avatars1.githubusercontent.com/u/44036562?s=280&v=4, " #github actions logo
            },
            "timestamp": str(datetime.datetime.now().isoformat()),
        }
    ]
}
#send failure message
r = requests.post(
    DISCORD_ENDPOINT,
    data=json.dumps(payload_json),
    headers={"Content-Type": "application/json"},
)
