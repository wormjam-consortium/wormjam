import yaml

name = input("Project Name: ")
organism = input("Organism: ")
short_name = input("Organism Short Name: ")
project_info = input("Project Description: ")
while True:
    db_table = input("Database Table (True/False): ")
    try:
        assert db_table.lower() in ["true","false"]
        db_table = db_table.capitalize()
        db_table = bool(db_table)
        break
    except Exception:
        print("Answer must be True or False.")

values = {
    "Project name": name,
    "Organism": organism,
    "Organism short name": short_name,
    "Project description": project_info,
    "Database table": db_table
}

with open(r".github\tests\config.yml", "w") as output_file:
    yaml.dump(values, output_file, sort_keys=False)