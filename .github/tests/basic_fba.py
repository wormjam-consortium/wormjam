import cobra

from pathlib import Path
from support.helper_classes import ModelConfig

settings_path = Path(".github") / "tests" / "config.yml"
settings = ModelConfig(settings_path)

model = cobra.io.read_sbml_model(f"{settings.name}.xml")
print("Model:")
print(len(model.reactions), "reactions")
print(len(model.metabolites), "metabolites")
print(len(model.genes), "genes")

biomass_rxn = model.reactions.get_by_id("BIO0100")
model.objective = biomass_rxn
medium = model.medium
medium["O2_Exchange_reactions_e"] = 1000
medium["EXC0050"] = 1000
model.medium = medium
with model:
    solution = model.optimize()
print("---------------------------------------------")
print("Solution:")
print(solution.objective_value)
print(solution.status)

assert solution.objective_value != 0, "Flux not carried in normal growth"
