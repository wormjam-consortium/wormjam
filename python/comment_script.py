import json
import sys
import os
import requests

API_KEY = sys.argv[1]
REPO_SLUG = sys.argv[2]
PULL_REQUEST = sys.argv[3]

API_ENDPOINT = "https://api.github.com/repos/%s/issues/%s/comments"%(REPO_SLUG,PULL_REQUEST)

print(API_KEY)
print(REPO_SLUG)
print(PULL_REQUEST)
print(API_ENDPOINT)

headers = {'Authorization':'token '+API_KEY}

def post_to_github(data):
    ddata = {"body":data}
    json_data = json.dumps(ddata)
    comment = requests.post(API_ENDPOINT,headers=headers,data=json_data) 
    if comment.status_code == 201:
        print("API Successful: "+data.split("\n")[0])

for i in os.listdir():
    if "results.txt" in i:
        with open(i) as f:
            # data = {"body":f.read()}
            raw = f.read()
    if "travis_wait" in i:
        with open(i) as f:
            errors = f.read()

        # json_data = json.dumps(data)

        # comment = requests.post(API_ENDPOINT,headers=headers,data=json_data)
        # if comment.status_code == 201:
        #     print("API Successful")


kill,raw = raw.split("============================= test session starts ==============================")
tests,raw = raw.split("=================================== FAILURES ===================================")
failures,other = raw.split("=============================== warnings summary ===============================")

##errors doesn't really need parsing

errors = errors.split("Model:")[0]

## Parsing Tests
tests = tests.split("\n")[7:]
tests = [i.split("::")[1].split(" [ ")[0] for i in tests if len(i) > 2]
tests = [i.split(" ")[1]+": " + i.split(" ")[0] for i in tests]
tests = "\n".join(tests)

## Parse Failures

failures = failures.split("\n")[1:-1]
failure_groups = []
intermediate = []
for line in failures:
    if line.startswith("_"):
        failure_groups.append(intermediate)
        intermediate = [line]
    else:
        intermediate.append(line)
failure_groups.append(intermediate)
failure_groups.pop(0)

for index,item in enumerate(failure_groups):
    lines = []
    assert_location = False
    for i in item:
        if i.startswith("E "):
            lines.append(item.index(i))
        if i.startswith("E assert") and not any("Captured stderr call" in j for j in item):
            assert_location = item.index(i)
    min_position = min(lines)
    if assert_location:
        cut = [item[0]]+item[min_position:assert_location]
        cut[0] = "**"+cut[0].replace(" ","").lstrip("_").rstrip("_").upper()+"**"
        failure_groups[index] = cut[0] + "\n" + " ".join([i[2:] for i in cut[1:]])
    else:
        cut = [item[0]]+item[min_position:]
        cut[0] = "**"+cut[0].replace(" ","").lstrip("_").rstrip("_").upper()+"**"
        output = cut[0]+"\n"
        for i in cut[1:]:
            if i.startswith("E "):
                output += i[2:]+" "
            else:
                output += "\n"+i
        failure_groups[index] = output

failures = "\n".join(failure_groups)
    
## Parse other



## Need to send: Errors, Tests, Failures, Broken, Metabolites




Errors = "### Errors\n"+errors
Tests = "### Tests\n"+tests
Failures = "### Failures\n"+failures
Broken="### Broken Tests\n"+other

for i in [Errors,Tests,Failures,Broken]: #VErrors,
    post_to_github(i)