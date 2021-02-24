import cobra

model = cobra.io.read_sbml_model("WormJam.xml")
print("Model:")
print(len(model.reactions), "reactions")
print(len(model.metabolites), "metabolites")
print(len(model.genes), "genes")

biomass_rxn = model.reactions.get_by_id("BIO0100")
model.objective = biomass_rxn
medium = model.medium
for i in medium:
    medium[i] = 0
model.medium = medium
with model:
    solution = model.optimize()
print("---------------------------------------------")
print("Solution:")
print(solution.objective_value)
print(solution.status)

assert solution.objective_value == 0, "Flux carried under restricted conditions"
