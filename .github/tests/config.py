import json

pipeline = {}

pipeline["name"] = input(
    "What should the SBML file be named? Do not include the .xml extension: "
)
pipeline["organism"] = input(
    "What is the name of the system? For example, Human Epithelial Cell or Caenorhabditis elegans: "
)
pipeline["short name"] = input("What is the abbreviated name? ")
pipeline["dbtable"] = input(
    "Are you using a databases table (Database-SBtab.tsv)? True/False: "
)
with open(r"travis/settings.json", "w+") as f:
    json.dump({"pipeline": pipeline}, f, indent=4)
