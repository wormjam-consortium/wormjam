#!/usr/bin/env python

import csv
import datetime
import json
import os
from pathlib import Path
from string import Template
import sys
import traceback
from lxml import etree

# debugging
from xml.etree import ElementTree

OUTPUT_LOCATION = Path("tmp")/"output"

def find_sbml_file(directory):
	"""Function to locate an SBML file.

	Args:
		directory (str): Directory containing the SBML file

	Returns:
		str: name of the SBML file. Note, this does not provide the path to the file
	"""	
	SBML_file = [f for f in os.listdir(directory) if f.endswith(".xml")]
	if len(SBML_file) > 1:
		print("WARNING: Multiple XML files found. Using largest file.")
		return sorted(SBML_file,key=lambda f: os.path.getsize(f),reverse=True)[0]
	elif len(SBML_file):
		return SBML_file[0]
	else:
		sys.exit("ERROR: No XML files found. Is this being run from the correct directory?")


def import_sbml_file(SBML_file):
	"""Imports an SBML xml file as a lxml etree Element

	Note that the essentially all information you'll want 
	is in the child attribute "model"

	Args:
		SBML_file (str): string path to the file

	Returns:
		lxml.etree.Element: an lxml etree element
	"""	
	tree = etree.parse(str(Path(os.getcwd())/SBML_file))
	sbml = tree.getroot()
	return sbml

def generate_headers(section):
	children = section.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)

	annotations = []
	notes = []
	for entity in children:
		entity_annotations = entity.xpath(".//sbml:annotation//dc:identifier",namespaces=nsmap)
		for resource in entity_annotations:
			if (resource:=resource.attrib["{%s}title"%nsmap["dc"]]) not in annotations:
				annotations.append(resource)
		entity_notes = entity.xpath(".//sbml:notes//xhtml:p",namespaces=nsmap)
		for note in entity_notes:
			if (note:=note.text.split(":")[0]) not in notes and note not in ['Comment', 'Curator']:
				notes.append(note)
	
	annotations = ["!Identifiers:"+a for a in annotations]
	notes = ["!Notes:"+n for n in notes]
	unused = section.xpath("./sbml:notes/xhtml:p",namespaces=nsmap)
	if len(unused):
		unused = unused[0].text.split("\n")[1:]
		annotations.extend(a for a in unused if a.startswith("!Identifiers"))
		notes.extend(a for a in unused if a.startswith("!Notes"))
	headers = sorted(annotations + notes)
	return headers

def pull_db_refs_from_sbml(dbs,entity):
	data = []
	notes = [e.text for e in entity.xpath("./sbml:notes//xhtml:p",namespaces=nsmap)]
	for i in dbs:
		search_term = i.split(":")[1]
		if "!Identifier" in i:
			ref = entity.xpath(".//dc:identifier[@dc:title='%s']"% search_term,namespaces=nsmap)
			if len(ref):
				ref = "|".join([i.attrib["{%s}subject" % nsmap["dc"]] for i in ref])
			else:
				ref = ""
		elif "!Notes" in i:
			ref = [note for note in notes if search_term == note.split(": ")[0]]
			if len(ref):
				ref = "|".join([i.split(": ")[1] for i in ref])
			else:
				ref = ""
		else:
			print("WARNING:",i,"is poorly labelled - missing !Identifier or !Notes")
			ref = ""
		data.append(ref)
	comments = [note for note in notes if "Comment" == note.split(": ")[0]]
	curator = [note for note in notes if "Curator" == note.split(": ")[0]]
	for i in [comments,curator]:
		if len(i):
			data.append(i[0].split(": ")[1])
		else:
			data.append("")
	return data

# load the file most likely to be the SBML model
# change os.getcwd() if file is not in the root directory of the github repo
sbml_file = find_sbml_file(os.getcwd())
sbml = import_sbml_file(sbml_file)

# see if an output directory already exists, otherwise make it

if not os.path.isdir(OUTPUT_LOCATION):
	os.makedirs(OUTPUT_LOCATION)

# build namespace map based on what is included in the SBML file
nsmap = {k:v for k,v in sbml.nsmap.items()} 
nsmap["sbml"]=nsmap.pop(None)

# access the model layer of the SBML file and set up SBtab variables
model = sbml.find("{%s}model" % nsmap["sbml"])

NAME = model.attrib["name"]
DATE = datetime.datetime.now().date().isoformat()
SBtabVersionString = "SBtabVersion='1.0'"
SBtabHeaderTemplate = Template("!!SBtab TableID='$TableID' Document='%s' TableType='$TableType' TableName='$TableName' %s Date='%s'" % (NAME,SBtabVersionString,DATE))
	
# CURATORS

# try to find curators
try:
	curators = model.xpath(
		".//vCard:vcards/vCard:vcard",
		namespaces = nsmap
		)

	# if curators were found
	if len(curators):
		# check what information is included in the SBML

		SBtab_header = SBtabHeaderTemplate.substitute(TableID="curator",TableType="Curator",TableName="Curators")
		headers = ["ID","!GivenName","!Surname","!Email","!OrganizationName"]
		with open(OUTPUT_LOCATION/"Curator-SBtab.tsv","w+",newline="") as f:
			curator_tsv = csv.writer(f,delimiter="\t")
			curator_tsv.writerow([SBtab_header])
			curator_tsv.writerow(headers)
			for vCard in curators:
				curator_tsv.writerow([
					vCard.attrib["{%s}about" % nsmap["rdf"]],
					vCard.xpath(".//vCard:given",namespaces=nsmap)[0].text,
					vCard.xpath(".//vCard:surname",namespaces=nsmap)[0].text,
					vCard.xpath(".//vCard:email",namespaces=nsmap)[0].text,
					vCard.xpath(".//vCard:org",namespaces=nsmap)[0].text
				])
		print("COMPLETE: Curators")
	else:
		print("SKIPPED: No curators found")
except Exception as e:
	sys.exit("ERROR: Processing Compartments\n"+str(e))

# GENES
try:
	genes = model.xpath(".//fbc:listOfGeneProducts",namespaces=nsmap)[0]
	if len(genes):
		SBtab_header = SBtabHeaderTemplate.substitute(TableID="compound",TableType="Gene",TableName="Genes")
		dbs = generate_headers(genes)
		headers = ["!ID","!Symbol","!LocusName","!Name"] + dbs + ["!Curator","!Comments"]
		with open(OUTPUT_LOCATION/"Gene-SBtab.tsv","w+",newline="") as f:
			gene_tsv = csv.writer(f,delimiter="\t")
			gene_tsv.writerow([SBtab_header])
			gene_tsv.writerow(headers)
			children = genes.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)
			for gene in children:
				data = [
					gene.attrib["metaid"],
					gene.attrib["{%s}name"%nsmap["fbc"]].split("@")[0],
					gene.attrib["{%s}name"%nsmap["fbc"]].split("@")[1].split("|")[0],
					gene.attrib["{%s}name"%nsmap["fbc"]].split("|")[1]
					]
				data.extend(pull_db_refs_from_sbml(dbs,gene))
				gene_tsv.writerow(data)
	print("COMPLETE: Genes")
except Exception as e:
	sys.exit("ERROR: Processing Genes\n"+str(e))

# PATHWAYS
try:
	pathways = model.xpath(".//groups:listOfGroups",namespaces=nsmap)[0]
	if len(pathways):
		SBtab_header = SBtabHeaderTemplate.substitute(TableID="pathway",TableType="Pathway",TableName="Pathways")
		dbs = generate_headers(pathways)
		headers = ["ID"] + dbs + ["!Curator","!Comments"]
		with open(OUTPUT_LOCATION/"Pathway-SBtab.tsv","w+",newline="") as f:
			pathway_tsv = csv.writer(f,delimiter="\t")
			pathway_tsv.writerow([SBtab_header])
			pathway_tsv.writerow(headers)
			children = pathways.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)
			for pathway in children:
				data = [
					pathway.attrib["metaid"]]
				data.extend(pull_db_refs_from_sbml(dbs,pathway))
				pathway_tsv.writerow(data)
	print("COMPLETE: Pathways")
except Exception as e:
	sys.exit("ERROR: Processing Pathways\n"+str(e))
# COMPARTMENTS

try:
	compartments = model.xpath(".//sbml:listOfCompartments",namespaces=nsmap)[0]
	if len(compartments):
		SBtab_header = SBtabHeaderTemplate.substitute(TableID="compartment",TableType="Compartment",TableName="Compartments")
		dbs = generate_headers(compartments)
		headers = ["ID","!Name","!Size","!spatialDimensions"] + dbs + ["!Curator","!Comments"]
		with open(OUTPUT_LOCATION/"Compartment-SBtab.tsv","w+",newline="") as f:
			compartment_tsv = csv.writer(f,delimiter="\t")
			compartment_tsv.writerow([SBtab_header])
			compartment_tsv.writerow(headers)
			children = compartments.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)
			for compartment in children:
				data = [
					compartment.attrib["metaid"],
					compartment.attrib["name"],
					compartment.attrib["size"],
					compartment.attrib["spatialDimensions"]]
				data.extend(pull_db_refs_from_sbml(dbs,compartment))
				compartment_tsv.writerow(data)
	print("COMPLETE: Compartments")
except Exception as e:
	traceback.print_exc(e)
	sys.exit("ERROR: Processing Compartments\n"+str(e))

# COMPOUND
try:
	compounds = model.xpath(".//sbml:listOfSpecies",namespaces=nsmap)[0]
	if len(compounds):
		SBtab_header = SBtabHeaderTemplate.substitute(TableID="compound",TableType="Compound",TableName="Compounds")
		dbs = generate_headers(compounds)
		headers = ["!ID","!Name","!Location","!Charge","!Formula","!IsConstant","!SBOTerm","!InitialConcentration","!hasOnlySubstanceUnits"] + dbs + ["!Curator","!Comments"]
		with open(OUTPUT_LOCATION/"Compound-SBtab.tsv","w+",newline="") as f:
			compound_tsv = csv.writer(f,delimiter="\t")
			compound_tsv.writerow([SBtab_header])
			compound_tsv.writerow(headers)
			children = compounds.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)
			for species in children:
				data = [
					species.attrib["metaid"],
					species.attrib["name"],
					species.attrib["compartment"],
					species.attrib["{%s}charge"%nsmap["fbc"]],
					species.attrib["{%s}chemicalFormula"%nsmap["fbc"]],
					species.attrib["constant"],
					species.attrib.get("sboTerm",""),
					species.attrib["initialConcentration"],
					species.attrib["hasOnlySubstanceUnits"]
					]
				data.extend(pull_db_refs_from_sbml(dbs,species))
				compound_tsv.writerow(data)
	print("COMPLETE: Compounds")
except Exception as e:
	sys.exit("ERROR: Processing Compounds\n"+str(e))

# PARAMETERS

try:
	parameters = model.xpath(".//sbml:listOfParameters",namespaces=nsmap)[0]
	if len(parameters):
		SBtab_header = SBtabHeaderTemplate.substitute(TableID="parameter",TableType="Parameter",TableName="Parameters")
		dbs = generate_headers(parameters)
		headers = ["!ID","!Parameter","!Value","!Unit","!Type","!SBOTerm"] + dbs + ["!Curator","!Comments"]
		with open(OUTPUT_LOCATION/"Parameter-SBtab.tsv","w+",newline="") as f:
			parameter_tsv = csv.writer(f,delimiter="\t")
			parameter_tsv.writerow([SBtab_header])
			parameter_tsv.writerow(headers)
			children = parameters.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)
			for parameter in children:
				data = [
					parameter.attrib["id"],
					parameter.attrib["id"],
					parameter.attrib["value"],
					"", #Unit is blank normally
					"global parameter",
					parameter.attrib.get("sboTerm","")
					]
				data.extend(pull_db_refs_from_sbml(dbs,parameter))
				parameter_tsv.writerow(data)
	print("COMPLETE: Parameters")
except Exception as e:
	sys.exit("ERROR: Processing Parameters\n"+str(e))

# REACTION
def extract_genes(fragment):
	if len(fragment.getchildren()):
		def clean_operator(tag):
			return tag.replace("{"+nsmap["fbc"]+"}","")
		operator = clean_operator(fragment.tag)
		linker = " "+operator+" "
		inner = linker.join([extract_genes(child) for child in fragment])
		if clean_operator(fragment.getparent().tag) not in [operator,"geneProductAssociation"] and clean_operator(fragment.tag) != "geneProductAssociation":
			inner = "("+inner+")"
		return inner
	else:
		try:
			return fragment.attrib["{%s}geneProduct"%nsmap["fbc"]].lstrip("G_")
		except:
			print(fragment)
			print(fragment.getparent().getparent().getparent().attrib["metaid"])
			exit()

def extract_formula(fragment):
	
	reactants = fragment.find("{%s}listOfReactants"%nsmap["sbml"])
	products = fragment.find("{%s}listOfProducts"%nsmap["sbml"])
	link = "<=>"
	
	if reactants is not None:
		reactants = reactants.xpath("./sbml:speciesReference",namespaces=nsmap)
		reactants = [react.attrib["species"] if int(float(react.attrib["stoichiometry"])) == 1 \
			else react.attrib["stoichiometry"] + " " + react.attrib["species"] for react in reactants]
		reactants = " + ".join(reactants)
	else:
		reactants = ""
	if products is not None:
		products = products.xpath("./sbml:speciesReference",namespaces=nsmap)
		products = [prod.attrib["species"] if int(float(prod.attrib["stoichiometry"])) == 1 \
			else prod.attrib["stoichiometry"] + " " + prod.attrib["species"] for prod in products]
		products = " + ".join(products)
	else:
		products = ""

	reaction_string = " ".join((reactants,link,products))
	return reaction_string

try:
	reactions = model.xpath(".//sbml:listOfReactions",namespaces=nsmap)[0]
	if len(reactions):
		SBtab_header = SBtabHeaderTemplate.substitute(TableID="reaction",TableType="Reaction",TableName="Reactions")
		dbs = generate_headers(reactions)
		headers = ["!ID","!Name","!ReactionFormula","!Location","!IsReversible","!GeneAssociation","!SBOTerm","!Pathway","!SuperPathway"] + dbs + ["!Curator","!Comments"]
		with open(OUTPUT_LOCATION/"Reaction-SBtab.tsv","w+",newline="") as f:
			reaction_tsv = csv.writer(f,delimiter="\t")
			reaction_tsv.writerow([SBtab_header])
			reaction_tsv.writerow(headers)
			children = reactions.xpath("./*[not(self::sbml:notes)]",namespaces=nsmap)
			for r in children:		
				# somewhat hacky way of extracting Pathway and Superpathway from the notes annotations
				pathways = r.xpath(".//xhtml:p[contains(text(),'Pathway:')]",namespaces=nsmap)
				pathways = [": ".join(p.text.split(": ")[1:]) for p in pathways]

				#get gene association
				genes = r.find("{%s}geneProductAssociation"%nsmap["fbc"])
				if genes is not None:
					genes = extract_genes(genes)
				else:
					genes = ""
				data = [
					r.attrib["metaid"],
					r.attrib["name"],
					extract_formula(r),
					r.attrib["compartment"],
					r.attrib["reversible"],
					genes,
					r.attrib.get("sboTerm",""),
					pathways[0],
					pathways[1]
					]
				data.extend([i for i in pull_db_refs_from_sbml(dbs,r) if "Pathway:" not in i]) #extra filter to prevent pathway double annotation
				reaction_tsv.writerow(data)
	print("COMPLETE: Reactions")
except Exception as e:
	sys.exit("ERROR: Processing Reactions\n"+str(e))

print("COMPLETE: Conversion completed!")