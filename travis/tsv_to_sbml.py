#!/usr/bin/env python

import csv
import datetime
import json
import os
import sys
import uuid
from copy import deepcopy

import pyparsing as pp
import requests
from lxml import etree

from helper_classes import ModelSystem

OUTPUT_NAME = "WormJam.xml"
BUILD = True
CLEAN_DELETION = False

# ## Comment out these two lines for local builds of the model
# DISCORD_ENDPOINT = sys.argv[1] #Discord Webhook endpoint, passed from Travis-CI
# TRAVIS_BUILD_NUMBER = sys.argv[2] #Travis Build Number, passed from Travis-CI


__author__ = "Jake Hattwell"
__copyright__ = "None"
__credits__ = ["Jake Hattwell"]
__license__ = "CCO"
__version__ = "1"
__maintainer__ = "Jake Hattwell"
__email__ = "j.hattwell@uq.edu.au"
__status__ = "Live"


######################
######################
## 
## Utility Functions
##
######################
######################

def genID():
    ##UUID generator
    return str(uuid.uuid4()).replace("-","_")

def annotate(db_dict, ref):
    """Function to access reference links, and handle when those links are not in DB table"""    
    if ref in db_dict:
        return db_dict[ref]["!IdentifiersOrgPrefix"]
    else:
        return "https://identifiers.org/"+ref

def check_db_type(db_dict,ref):
    """Function to access reference links, and handle when those links are not in DB table"""    
    if ref in db_dict:
        return db_dict[ref]["!IsOrIn"]
    else:
        return "Is"

def gen_annotation_tree(parent, db_dict, data):
    #get a list of which DBs are annotated for this entry, as well as what type of DB they are
    annotated_dbs = [db.split(":")[1] for db in data.keys() if "!Identifiers" in db and data[db] != ""]
    db_types = [check_db_type(db_dict,db) for db in annotated_dbs]
    # create bqbiol:type -> rdf:bag -> rdf:li elements 
    if "Is" in db_types:
        bqbiol_is_and_rdf_bag = etree.SubElement(etree.SubElement(parent,"{%s}"%NS_MAP["bqbiol"]+"is"),"{%s}"%NS_MAP["rdf"]+"Bag")
    if "In" in db_types:
        bqbiol_occurs_in_and_rdf_bag = etree.SubElement(etree.SubElement(parent,"{%s}"%NS_MAP["bqbiol"]+"isPartOf"),"{%s}"%NS_MAP["rdf"]+"Bag")

    #annotate to the correct bag
    for db in annotated_dbs:
        if check_db_type(db_dict,db) == "Is":
            for identifier in data["!Identifiers:"+db].split("|"):
                etree.SubElement(bqbiol_is_and_rdf_bag,"{%s}"%NS_MAP["rdf"]+"li",attrib={"{%s}"%NS_MAP["rdf"]+"resource":annotate(db_dict,db)+":"+identifier})
        else:
            for identifier in data["!Identifiers:"+db].split("|"): 
                etree.SubElement(bqbiol_occurs_in_and_rdf_bag,"{%s}"%NS_MAP["rdf"]+"li",attrib={"{%s}"%NS_MAP["rdf"]+"resource":annotate(db_dict,db)+":"+identifier})



## Load settings
print("Build model is set to",BUILD)
settings = json.load(open("travis/settings.json","r"))["pipeline"]

    

## Load tsv files
compiler = ModelSystem()
compiler.load_folder("curation")

metabolite_validation = compiler.validate_rxn_mets() #check that all required metabolites are included in the model

if settings["dbtable"]:
    db_dict = compiler.tables.get("Database").data
else:
    db_dict = {}

try:
    assert len(metabolite_validation) == 0, "Missing metabolites"
except:
    text = "Reaction: Missing Metabolites"
    for key,val in metabolite_validation.items():
        text += "\n"+key+": " + ", ".join(val)
    payload_json = {
        "embeds": [{
            "title": "WormJam CI Report",
            "color": 10027008,
            "description": "Missing Metabolites - Build aborted",
            "fields":[
                {
                    "name": "Build Number",
                    "value":str(TRAVIS_BUILD_NUMBER)
                },
                {
                    "name":"Notes",
                    "value":text
                }
            ],
            "thumbnail": {
                "url": "https://travis-ci.com/images/logos/Tessa-1.png"
            },
            "timestamp": str(datetime.datetime.now().isoformat())
        }]
    }
    r =requests.post(DISCORD_ENDPOINT,data=json.dumps(payload_json), headers={"Content-Type": "application/json"})
    exit(1)

#only include genes that are involved in regulation of reactions in the SBML model
active_gene_list = []
for key,val in compiler.tables.get("Reaction").data.items():
    genes = val["!GeneAssociation"].split(" ")
    genes = [i.replace("(","").replace(")","") for i in genes]
    while "and" in genes:
        genes.remove("and")
    while "or" in genes:
        genes.remove("or")
    active_gene_list.extend(genes)
active_gene_list = set(active_gene_list)
print(len(active_gene_list))

######################
######################
## 
## Build Model
##
######################
######################
if BUILD:
    output_model = open(OUTPUT_NAME,"wb")

#define xml namespaces for inclusion
NS_MAP = {
    'fbc': "http://www.sbml.org/sbml/level3/version1/fbc/version2",
    'groups':"http://www.sbml.org/sbml/level3/version1/groups/version1",
    'xhtml':"http://www.w3.org/1999/xhtml",
    'rdf':"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
    'dc':"http://purl.org/dc/elements/1.1/",
    'vCard':"http://www.w3.org/2001/vcard-rdf/3.0#",
    'dcterms':"http://purl.org/dc/terms/",
    'bqbiol':"http://biomodels.net/biology-qualifiers/",
    None: "http://www.sbml.org/sbml/level3/version1/core"} #This is just a catcher/default namespace

#create sbml structure
sbml = etree.Element("sbml",metaid=genID(),attrib={"{%s}"%NS_MAP["fbc"]+"required":"false","{%s}"%NS_MAP["groups"]+"required":"false"},nsmap=NS_MAP)
other_attribs = {
    "level":"3",
    "version":"1",
}
for key,val in other_attribs.items():
    sbml.set(key,val)

#create model structure
#customisation goes here
#id = 
#name = 
#desc = 
model = etree.SubElement(sbml,"model",id="WormJamTestBuild",attrib={"{%s}"%NS_MAP["fbc"]+"strict":"false"},metaid=genID(),name="WormJam Draft Model")
model_notes = etree.SubElement(model,"notes")
model_notes_desc = etree.SubElement(model_notes,"{%s}"%NS_MAP["xhtml"]+"p")
model_notes_desc.text="Genome Scale Model of the organism Caenorhabditis elegans"

#
# curators
# We store the curator information within the model's annotation
# Need to add in that curators do get mentioned in the annotation package
#

model_annotation = etree.SubElement(model,"annotation")
model_annotation_RDF = etree.SubElement(model_annotation,"{%s}"%NS_MAP["rdf"]+"RDF")
# In this script, I nest much of the XML structure creation
# rdf:Description -> dc:creator -> rdf:Bag == This bag holds lists. Each list contains info about a curator.
model_annotation_RDF_description_DC_bag = etree.SubElement(etree.SubElement(etree.SubElement(model_annotation_RDF,"{%s}"%NS_MAP["rdf"]+"Description",attrib={"{%s}"%NS_MAP["rdf"]+"about":"#"+model.get("metaid")}),"{%s}"%NS_MAP["dc"]+"creator"),"{%s}"%NS_MAP["rdf"]+"Bag")

for key,val in compiler.tables.get("Curator").data.items():
    rdf_li = etree.SubElement(model_annotation_RDF_description_DC_bag,"{%s}"%NS_MAP["rdf"]+"li",attrib={"{%s}"%NS_MAP["rdf"]+"about":key,"{%s}"%NS_MAP["rdf"]+"parseType":"Resource"})
    vCard_N = etree.SubElement(rdf_li,"{%s}"%NS_MAP["vCard"]+"N",attrib={"{%s}"%NS_MAP["rdf"]+"parseType":"Resource"})
    etree.SubElement(vCard_N,"{%s}"%NS_MAP["vCard"]+"Family").text = val["!family-name"]
    etree.SubElement(vCard_N,"{%s}"%NS_MAP["vCard"]+"Given").text = val["!given-name"]
    etree.SubElement(rdf_li,"{%s}"%NS_MAP["vCard"]+"EMAIL").text = val["!email"]
    vCard_ORG = etree.SubElement(rdf_li,"{%s}"%NS_MAP["vCard"]+"ORG",attrib={"{%s}"%NS_MAP["rdf"]+"parseType":"Resource"})
    etree.SubElement(vCard_ORG,"{%s}"%NS_MAP["vCard"]+"Orgname").text = val["!organization-name"]



#
# genes
# 
#

model_listOfGeneProducts = etree.SubElement(model,"{%s}"%NS_MAP["fbc"]+"listOfGeneProducts")

for key,val in compiler.tables.get("Gene").data.items():
    if key in active_gene_list: #filter for only used genes
        attribs = {
            "{%s}"%NS_MAP["fbc"]+"id":"G_"+key,
            "{%s}"%NS_MAP["fbc"]+"label":key,
            "{%s}"%NS_MAP["fbc"]+"name":val["!Locus"],
            "metaid":key.replace(" ","_")
        }
        fbc_gene_prod = etree.SubElement(model_listOfGeneProducts,"{%s}"%NS_MAP["fbc"]+"geneProduct",attrib=attribs)
        annotation = etree.SubElement(fbc_gene_prod,"annotation")
        rdf_RDF = etree.SubElement(annotation,"{%s}"%NS_MAP["rdf"]+"RDF")
        rdf_desc = etree.SubElement(rdf_RDF,"{%s}"%NS_MAP["rdf"]+"Description",attrib={"{%s}"%NS_MAP["rdf"]+"about":"#"+attribs["metaid"]})
        gen_annotation_tree(rdf_desc,db_dict,val)      

#
# Pathways
#
model_listOfGroups = etree.SubElement(model,"{%s}"%NS_MAP["groups"]+"listOfGroups")

for key,val in compiler.tables.get("Pathway").data.items():
    attribs = {
        "{%s}"%NS_MAP["groups"]+"id":"P_"+key.replace(" ","_"),
        "{%s}"%NS_MAP["groups"]+"kind":"partonomy",
        "{%s}"%NS_MAP["groups"]+"name":key,
        "metaid":key.replace(" ","_")
    }
    groups_group = etree.SubElement(model_listOfGroups,"{%s}"%NS_MAP["groups"]+"group",attrib=attribs)
    g_annotation = etree.SubElement(groups_group,"annotation")
    g_rdf_desc = etree.SubElement(etree.SubElement(g_annotation,"{%s}"%NS_MAP["rdf"]+"RDF"),"{%s}"%NS_MAP["rdf"]+"Description",attrib={"{%s}"%NS_MAP["rdf"]+"about":"#"+attribs["metaid"]})
    #annotate
    gen_annotation_tree(g_rdf_desc,db_dict,val)
    #insert group members
    g_listOfMembers = etree.SubElement(groups_group,"{%s}"%NS_MAP["groups"]+"listOfMembers")
    listOfMembers = [rxn for rxn,info in compiler.tables.get("Reaction").data.items() if info["!Pathway"] == key]
    for i in listOfMembers:
        etree.SubElement(g_listOfMembers,"{%s}"%NS_MAP["groups"]+"member",attrib={"{%s}"%NS_MAP["groups"]+"id":"GM_"+i,"{%s}"%NS_MAP["groups"]+"idRef":i})


#
# Compartments
#
model_compartment_tree = etree.SubElement(model,"listOfCompartments")

for key,val in compiler.tables.get("Compartment").data.items():
    metaid = key.replace(" ","_")
    #fairly straightforward annotation
    compartment = etree.SubElement(model_compartment_tree,"compartment",attrib={"constant":"true","id":key,"metaid":metaid,"name":val["!Name"],"size":"1","spatialDimensions":str(val["!spatialDimensions"])})

    annotation = etree.SubElement(compartment,"annotation")
    cmpt_rdf_desc = etree.SubElement(etree.SubElement(annotation,"{%s}"%NS_MAP["rdf"]+"RDF"),"{%s}"%NS_MAP["rdf"]+"Description",attrib={"{%s}"%NS_MAP["rdf"]+"about":"#"+metaid})
    # annotate
    gen_annotation_tree(cmpt_rdf_desc,db_dict,val)

#
# Species
#

model_species_tree = etree.SubElement(model,"listOfSpecies")

for key,val in compiler.tables.get("Compound").data.items():
    attribs = {
        "boundaryCondition":"false",
        "compartment":val["!Location"],
        "constant":"false",
        "{%s}"%NS_MAP["fbc"]+"charge":val["!Charge"],
        "{%s}"%NS_MAP["fbc"]+"chemicalFormula":val["!Formula"],
        "hasOnlySubstanceUnits":"false",
        "id":key,
        "initialConcentration":val.get("!initialConcentration","0"),
        "name":"!Name"
    }
    if attribs["{%s}"%NS_MAP["fbc"]+"charge"] == "":
        attribs["{%s}"%NS_MAP["fbc"]+"charge"] = "0"
    metaid = key.replace(" ","_")
    metabolite = etree.SubElement(model_species_tree,"species",metaid=metaid,attrib=attribs)
    notes_body = etree.SubElement(etree.SubElement(metabolite,"notes"),"{%s}"%NS_MAP["xhtml"]+"body")
    for i in [key2 for key2 in list(val.keys()) if all(block not in key2 for block in ["!Identifiers","!Formula","!Charge"])]:
        if val[i]!="":
            if key=="!Charge" and val[i]=="":
                val[i] == "0" #small fix to change a blank charge to a charge of 0
            etree.SubElement(notes_body,"{%s}"%NS_MAP["xhtml"]+"p").text=i.replace("!","").replace("Notes:","").upper() + ": " + val[i]
    annotation_tree = etree.SubElement(etree.SubElement(etree.SubElement(metabolite,"annotation"),"{%s}"%NS_MAP["rdf"]+"RDF"),"{%s}"%NS_MAP["rdf"]+"Description",attrib={"{%s}"%NS_MAP["rdf"]+"about":"#"+metaid})
    gen_annotation_tree(annotation_tree,db_dict,val)

#
# Parameters
#

parameter_tree = etree.SubElement(model,"listOfParameters")
etree.SubElement(parameter_tree,"parameter",attrib={"constant":"true","id":"LOWER_BOUND","value":"-1000"})
etree.SubElement(parameter_tree,"parameter",attrib={"constant":"true","id":"ZERO_BOUND","value":"0"})
etree.SubElement(parameter_tree,"parameter",attrib={"constant":"true","id":"UPPER_BOUND","value":"1000"})

#
# Reactions
#


# GPR helper functions

def process_gene_association(gene_association):
    """Function that converts a string gene association,
    then converts it to XML using pyparsing, then reformats that xml
    into a SBML style geneProductAssociation"""

    if gene_association=='':
        return None
    identifier = pp.Word(pp.alphas, pp.alphanums + "_" + ".")("gene")
    comparison_term = identifier
    AND_ = pp.Keyword("and")("operator")
    OR_ = pp.Keyword("or")("operator")
    NOT_ = pp.Keyword("not")("operator")
    expr = pp.operatorPrecedence(comparison_term,[
                                # (NOT_, 1, pp.opAssoc.RIGHT, ),
                                (AND_, 2, pp.opAssoc.LEFT, ),
                                (OR_, 2, pp.opAssoc.LEFT, ),
                                ])
    #HACK
    expr.expr.resultsName = "group"

    try:
        out = expr.parseString(gene_association)
    except:
        #GA probably missing, return false to trigger warning
        return False


    text = out.asXML("expression")
    GA_tree = etree.fromstring(text)

    ##Expression containing group
    gpr = etree.Element("{%s}"%NS_MAP["fbc"]+"geneProductAssociation")
    ##dive into GA_tree, building GPR along the way
    group = GA_tree[0]
    if "operator" not in [element.tag for element in list(group)]:
        #case 1, single gene
        etree.SubElement(gpr,"{%s}"%NS_MAP["fbc"]+"geneProductRef",attrib={"{%s}"%NS_MAP["fbc"]+"geneProduct":"G_"+list(group)[0].text})
    else:
        #complex case
        def genHead(booltype):
            #help function to generate the and/or xml field
            if booltype == "or":
                branch = etree.Element("{%s}"%NS_MAP["fbc"]+"or",attrib={"sboTerm":"SBO:0000174"})
            else:
                branch = etree.Element("{%s}"%NS_MAP["fbc"]+"and",attrib={"sboTerm":"SBO:0000173"})
            return branch

        # Recursion explanation for when I inevitably need to reuse this

        # DIVE FUNC START - Accepts Element that has children
        # Create a list of genes to hold Elements that are generated
        # Check what operator this group of elements is linked by (AND/OR)
        # For each element:
        # 	Check if any child that has an operator tag, indicating that it contains nested children
        # 	If children with nested:
        # 		RECURSIVELY move to examining the group of elements belonging to the child 
        #		Attach the results of the examination to the gene list 
        # 	Else, if no children have operator children, then this must be the deepest level of this branch:
        # 		Create elements of each gene and append to gene list
        # If we are outside of the for loop now, it means that everything deeper than this level is in genelist.
        # Create an AND/OR element, and add everything in gene list as it's children, and return it
        
        def dive(xml_fragment):
            genes = []
            operator = [op.text for op in list(xml_fragment) if op.tag=="operator"][0]
            for element in list(xml_fragment):
                hits = [child for child in list(element) if child.tag == "operator"]				
                if len(hits) > 0:
                    genes.append(dive(element))
                else:
                    if element.text not in ['and','or']:
                        e = etree.Element("{%s}"%NS_MAP["fbc"]+"geneProductRef",attrib={"{%s}"%NS_MAP["fbc"]+"geneProduct":"G_"+element.text})
                        genes.append(e)
            
            op_xml = genHead(operator)
            for gene in genes:
                op_xml.append(gene)
            return op_xml


        gpr.append(dive(group))
    return gpr
#END XML PARSING

    ##reaction string handling
def react_proc(rxn):
    r,p = rxn.split("<=>")
    def quick(frag):
        frag = frag.split("+")
        frag = [i.rstrip().lstrip() for i in frag]
        frag = [i.split(" ") for i in frag]
        return frag
    r = quick(r)
    p = quick(p)
    #packaging
    reactants = {(i[1] if len(i) == 2 else i[0]):(i[0] if len(i)==2 else "1") for i in r}
    products = {(i[1] if len(i) == 2 else i[0]):(i[0] if len(i)==2 else "1") for i in p}
    for d in [reactants,products]:
        for key,val in d.items():
            try:
                d[key] = str(float(val))
            except:
                pass

    return (reactants,products)   




#### Actually doing the reactions
 
reaction_tree = etree.SubElement(model,"listOfReactions")


ignore = ["!Identifiers:kegg.reaction","!Identifiers:rheadb_exact","!Identifiers:rheadb_fuzzy","!Identifiers:pubmed","!Identifiers:doi","!Identifiers:eco",
"!Authors","!ReactionFormula","!SuperPathway","!Name","!IsReversible"]

for key,val in compiler.tables.get("Reaction").data.items():
    metaid = key.replace(" ","_")
    attribs = {
        "fast":"false",
        "reversible":val["!IsReversible"].lower(),
        "metaid":metaid,
        "id":key,
        "name":val["!Name"],
        "{%s}"%NS_MAP["fbc"]+"upperFluxBound":"UPPER_BOUND"
    }
    if attribs["reversible"] == "true":
        attribs["{%s}"%NS_MAP["fbc"]+"lowerFluxBound"] = "LOWER_BOUND"
    else:
        attribs["{%s}"%NS_MAP["fbc"]+"lowerFluxBound"] = "ZERO_BOUND"
    reaction_field = etree.SubElement(reaction_tree,"reaction",attrib=attribs)
    notes_body = etree.SubElement(etree.SubElement(reaction_field,"notes"),"{%s}"%NS_MAP["xhtml"]+"body")
    for i in [key2 for key2 in list(val.keys()) if all(block not in key2 for block in ["!Identifiers","!ReactionFormula"])]:
        if val[i]!="":
            etree.SubElement(notes_body,"{%s}"%NS_MAP["xhtml"]+"p").text=i.replace("!","").replace("Notes:","").replace("Pathway","Subsystem").upper() + ": " + val[i]


    annotation_tree = etree.SubElement(etree.SubElement(etree.SubElement(reaction_field,"annotation"),"{%s}"%NS_MAP["rdf"]+"RDF"),"{%s}"%NS_MAP["rdf"]+"Description",attrib={"{%s}"%NS_MAP["rdf"]+"about":"#"+metaid})
    gen_annotation_tree(annotation_tree, db_dict, val)
    try:
        reaction_field.append(process_gene_association(val["!GeneAssociation"]))
    except Exception as e:
        pass
    
    reactants,products = react_proc(val["!ReactionFormula"])
    if "" not in reactants:
        listOfReactants = etree.SubElement(reaction_field,"listOfReactants")
        for key2,val2 in reactants.items():
            etree.SubElement(listOfReactants,"speciesReference",attrib={"constant":"true","species":key2,"stoichiometry":val2})
    if "" not in products:       
        listOfProducts = etree.SubElement(reaction_field,"listOfProducts")
        for key2,val2 in products.items():
            etree.SubElement(listOfProducts,"speciesReference",attrib={"constant":"true","species":key2,"stoichiometry":val2})



######################
######################
## 
## Output
##
######################
######################
if BUILD:
    output_model.write(etree.tostring(sbml,encoding="UTF-8",standalone=False,xml_declaration=True,pretty_print=True))
    output_model.close()