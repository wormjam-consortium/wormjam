import yaml

name = input("Project Name: ")
organism = input("Organism: ")
short_name = input("Organism Short Name: ")
project_info = input("Project Description: ")
db_table = input("Database Table: ")

values = {
    "Project name": name,
    "Organism": organism,
    "Organism short name": short_name,
    "Project description": project_info,
    "Database table": db_table
}

with open(r".github\tests\config.yml", "w") as output_file:
    yaml.dump(values, output_file, sort_keys=False)