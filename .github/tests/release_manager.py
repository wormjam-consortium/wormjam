import json
import sys
import requests
import datetime

from pathlib import Path
from support.helper_classes import ModelConfig

settings_path = Path(".github") / "tests" / "config.yml"
settings = ModelConfig(settings_path)

DISCORD_ENDPOINT = sys.argv[1]  # Github Actions Channel Webhook
GITHUB_REPO_SLUG = sys.argv[2]  # user/repo
GITHUB_REPO_BRANCH = sys.argv[3].split("/")[
    -1
]  # branch - process the string and grab the last term

timestamp = datetime.datetime.now().strftime("%Y_%m_%d__%H_%M_%S")
filename = f"{settings.name}" + timestamp + ".tar.gz"

# prepare files for sending
packaged_model_file = {filename: open(f"{settings.name}.tar.gz", "rb")}

# construct embed to send to discord
# This embed is used for both messages
payload_json = {
    "embeds": [
        {
            "title": f"{settings.name} Release",
            "color": 2132223,  # this is github action colour
            "description": "Release from [%s](%s) for model version %s"
            % (
                GITHUB_REPO_SLUG,
                "https://github.com/" + GITHUB_REPO_SLUG,
                GITHUB_REPO_BRANCH,
            ),
            "fields": [
                {"name": "Version", "value": str(GITHUB_REPO_BRANCH)},
            ],
            "thumbnail": {
                "url": "https://avatars1.githubusercontent.com/u/44036562?s=280&v=4"  # github actions logo
            },
            "timestamp": str(datetime.datetime.now().isoformat()),
        }
    ]
}
# Send the report
r = requests.post(
    DISCORD_ENDPOINT,
    data=json.dumps(payload_json),
    headers={"Content-Type": "application/json"},
)
r2 = requests.post(DISCORD_ENDPOINT, files=packaged_model_file)
