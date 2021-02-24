import json
from memote.suite.api import test_model
import cobra

model = cobra.io.read_sbml_model("WormJam.xml")
code, results = test_model(
    model, sbml_version=(3, 1), results=True
)  # ,skip=["test_consistency"]
with open("results.json", "w+") as f:
    f.write(json.dumps(results, indent=4))
print("Memote Done")
