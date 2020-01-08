import csv
import os
import uuid
from copy import deepcopy

import pyparsing
from lxml import etree

OUTPUT_NAME = "WormJam.xml"

#!/usr/bin/env python

"""SBTabReader.py - converts a csv with WormBase RNAi identifiers in the first column to a list of corresponding target genes

Requires (use pip to install):
openpyxl
"""


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
## Helper Classes
##
######################
######################

class modelSystem():
    """Class for reading SBtab files
    """
    def __init__(self):
        self.tables = {}
        self.size = {}
    
    def loadTable(self,name,location):
        self.tables[name] = dataset(location)
        self.size[name] = self.tables[name].rows-2

    def load_folder(self,name,filetype):
        success = False
        print("------------------------")
        if os.path.isdir(name) == False:
            print("This folder does not exist")

        else:
            print("Folder loaded")
            paths = []
            for f in os.listdir(name):
                if filetype == "tsv":
                    if "SBtab.tsv" in f:
                        filename = f.replace("-SBtab.tsv","")
                        paths.append(filename)

            assert paths!= [],"There were no SBtab files found in "+name
            print("SBtab files found! Loading now!")
            self.count=1
            for hit in paths:
                print(" ".join(["Loading file:",hit]))
                self.loadTable(hit,name+"/"+hit+"-SBtab.tsv")

            print(" ".join([str(len(paths)),"files loaded into the model"]))
            success = True    
            

class dataset:
    """Importable class for loading SBTab files\nConverts SBTab as nested dictionary.\n

    instance.data = Dictionary of entries in SBTab\n
    Each entry is a dictionary of the data associated with that entry, with column headers as keys.
        
        Arguments:
            xlsx {str} -- Path to SBTab file of interest.
        
        Keyword Arguments:
            headerRow {int} -- Excel row of the header information, (default: {2})
            mode {str} -- version of dataset to load
        """

    def __init__(self,filename,headerRow=2,mode="xslx"):
        """Loads the SBTab file"""
        self.name = filename
        with open(filename,encoding="utf-8") as tsvfile:
            tsv = csv.reader(tsvfile,delimiter="\t")
            entries = []
            for row in tsv:
                if tsv.line_num == 1: #row 1 - SBtab DocString
                    self.sbString = row[0]
                elif tsv.line_num == 2: #row 2 - headers of the table
                    self.headers = row
                else:
                    entries.append(row)
            # define size of data
            self.cols = len(self.headers)
            self.rows = len(entries)+2
            # create the nested dict object
            try:
                self.data = {entry[0]:{self.headers[i]:(entry[i] if len(entry) >= len(self.headers) else '') for i in range(1,len(self.headers))} for entry in entries}
                while '' in self.data:
                    self.data.pop('')
            except:
                print(self.name)
                print("tsv import failed. Aborting...")
                exit()
            #remove blank entries


######################
######################
## 
## Utility Functions
##
######################
######################

def genID():
    return str(uuid.uuid4())

######################
######################
## 
## Load tsv files
##
######################
######################


compiler = modelSystem()
compiler.load_folder("curation","tsv")

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

output_model = open(OUTPUT_NAME,"wb")

#define xml namespaces
xmlns = "http://www.sbml.org/sbml/level3/version1/core"
fbc="http://www.sbml.org/sbml/level3/version1/fbc/version2"
groups="http://www.sbml.org/sbml/level3/version1/groups/version1"
xhtml="http://www.w3.org/1999/xhtml"
rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
dc="http://purl.org/dc/elements/1.1/"
vCard="http://www.w3.org/2001/vcard-rdf/3.0#"
dcterms="http://purl.org/dc/terms/"
bqbiol="http://biomodels.net/biology-qualifiers/"

NS_MAP = {
    'fbc': fbc,
    'groups':groups,
    'xhtml':xhtml,
    'rdf':rdf,
    'dc':dc,
    'vCard':vCard,
    'dcterms':dcterms,
    'bqbiol':bqbiol,
    None: xmlns}

#create sbml structure

sbml = etree.Element("sbml",metaid=genID(),attrib={"{%s}"%fbc+"required":"false","{%s}"%groups+"required":"false"},nsmap=NS_MAP)
other_attribs = {
    "level":"3",
    "version":"1",
}
for key,val in other_attribs.items():
    sbml.set(key,val)

model = etree.SubElement(sbml,"model",id="WormJamTestBuild",attrib={"{%s}"%fbc+"strict":"false"},metaid=genID(),name="WormJam Draft Model")
model_notes = etree.SubElement(model,"notes")
model_notes_desc = etree.SubElement(model_notes,"{%s}"%xhtml+"p")
model_notes_desc.text="Genome Scale Model of the organism Caenorhabditis elegans"

#
# curators
#

model_annotation = etree.SubElement(model,"annotation")
model_annotation_RDF = etree.SubElement(model_annotation,"{%s}"%rdf+"RDF")
model_annotation_RDF_description_DC_bag = etree.SubElement(etree.SubElement(etree.SubElement(model_annotation_RDF,"{%s}"%rdf+"Description",attrib={"{%s}"%rdf+"about":"#"+model.get("metaid")}),"{%s}"%dc+"creator"),"{%s}"%rdf+"Bag")

for key,val in compiler.tables.get("Curator").data.items():
    rdf_li = etree.SubElement(model_annotation_RDF_description_DC_bag,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"parseType":"Resource"})
    vCard_N = etree.SubElement(rdf_li,"{%s}"%vCard+"N",attrib={"{%s}"%rdf+"parseType":"Resource"})
    etree.SubElement(vCard_N,"{%s}"%vCard+"Family").text = val["!family-name"]
    etree.SubElement(vCard_N,"{%s}"%vCard+"Given").text = val["!given-name"]
    etree.SubElement(rdf_li,"{%s}"%vCard+"EMAIL").text = val["!email"]
    vCard_ORG = etree.SubElement(rdf_li,"{%s}"%vCard+"ORG",attrib={"{%s}"%rdf+"parseType":"Resource"})
    etree.SubElement(vCard_ORG,"{%s}"%vCard+"Orgname").text = val["!organization-name"]
#
# genes
# I should add a gene filter here probably to prevent export of EVERY gene in the model
#
model_listOfGeneProducts = etree.SubElement(model,"{%s}"%fbc+"listOfGeneProducts")

for key,val in compiler.tables.get("Gene").data.items():
    if key in active_gene_list: #filter for only used genes
        attribs = {
            "{%s}"%fbc+"id":"G_"+key,
            "{%s}"%fbc+"label":key,
            "{%s}"%fbc+"name":val["!Locus"],
            "metaid":genID()
        }
        fbc_gene_prod = etree.SubElement(model_listOfGeneProducts,"{%s}"%fbc+"geneProduct",attrib=attribs)
        annotation = etree.SubElement(fbc_gene_prod,"annotation")
        rdf_RDF = etree.SubElement(annotation,"{%s}"%rdf+"RDF")
        rdf_desc = etree.SubElement(rdf_RDF,"{%s}"%rdf+"Description",attrib={"{%s}"%rdf+"about":"#"+attribs["metaid"]})
        rdf_bag_and_bqbio_is = etree.SubElement(etree.SubElement(etree.SubElement(rdf_desc,"{%s}"%bqbiol+"is"),"{%s}"%rdf+"Bag"),"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":"http://identifiers.org/wormbase/"+key})
        if val["!GO_process"] != "":
            rdf_bqbiol_occurs_in_bag = etree.SubElement(etree.SubElement(rdf_desc,"{%s}"%bqbiol+"occursIn"),"{%s}"%rdf+"Bag")
            for i in val["!GO_process"].split(";"):
                etree.SubElement(rdf_bqbiol_occurs_in_bag,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":"http://identifiers.org/go/"+i})
#
# Pathways
#
group_tree = etree.SubElement(model,"{%s}"%groups+"listOfGroups")

for key,val in compiler.tables.get("Pathway").data.items():
    attribs = {
        "{%s}"%groups+"id":"P_"+key.replace(" ","_"),
        "{%s}"%groups+"kind":"partonomy",
        "{%s}"%groups+"name":key,
        "metaid":genID()
    }
    groups_group = etree.SubElement(group_tree,"{%s}"%groups+"group",attrib=attribs)
    descriptors = [val["!Identifiers:GO_process"],val["!Identifiers:kegg:pathway"],val["!Identifiers:BioCyc"],val["!Identifiers:pw"]]
    links = ["http://identifiers.org/go/","http://identifiers.org/kegg:","http://identifiers.org/biocyc/","http://identifiers.org/pw/"]
    merge = zip(links,descriptors)
    new = []
    for i in merge:
        if i[1] != "":
            ids = i[1].replace(" ","").split(";")
            ids = [i[0] + j for j in ids]
            new += ids
    if new != []:
        annotation = etree.SubElement(groups_group,"annotation")
        rdf_desc = etree.SubElement(etree.SubElement(annotation,"{%s}"%rdf+"RDF"),"{%s}"%rdf+"Description",attrib={"{%s}"%rdf+"about":"#"+attribs["metaid"]})
        is_bag = etree.SubElement(etree.SubElement(rdf_desc,"{%s}"%bqbiol+"is"),"{%s}"%rdf+"Bag")
        for i in new:
            etree.SubElement(is_bag,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":i})
    listOfMembers = [rxn for rxn,info in compiler.tables.get("Reaction").data.items() if info["!Pathway"] == key]
    group_listOfMemebers = etree.SubElement(groups_group,"{%s}"%groups+"listOfMembers")
    for i in listOfMembers:
        etree.SubElement(group_listOfMemebers,"{%s}"%groups+"member",attrib={"{%s}"%groups+"id":"GM_"+i,"{%s}"%groups+"idRef":i})

#
# Compartments
#
compartment_tree = etree.SubElement(model,"listOfCompartments")

for key,val in compiler.tables.get("Compartment").data.items():
    metaid = genID()
    compartment = etree.SubElement(compartment_tree,"compartment",attrib={"constant":"true","id":key,"metaid":metaid,"name":val["!Name"],"size":"1","spatialDimensions":"3"})
    if val["!Comment"] != "":
        etree.SubElement(etree.SubElement(compartment,"notes"),"{%s}"%xhtml+"p").text = val["!Comment"]
    if val["!Identifiers:go"] != "":
        annotation = etree.SubElement(compartment,"annotation")
        rdf_desc = etree.SubElement(etree.SubElement(annotation,"{%s}"%rdf+"RDF"),"{%s}"%rdf+"Description",attrib={"{%s}"%rdf+"about":"#"+metaid})
        is_bag = etree.SubElement(etree.SubElement(rdf_desc,"{%s}"%bqbiol+"is"),"{%s}"%rdf+"Bag")
        etree.SubElement(is_bag,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":"http://identifiers.org/go/"+val["!Identifiers:go"]})

#
# Species
#


species_tree = etree.SubElement(model,"listOfSpecies")

for key,val in compiler.tables.get("Compound").data.items():
    attribs = {
        "boundaryCondition":"false",
        "compartment":val["!Location"],
        "constant":"false",
        "{%s}"%fbc+"charge":val["!Charge"],
        "{%s}"%fbc+"chemicalFormula":val["!Formula"],
        "hasOnlySubstanceUnits":"false",
        "id":key,
        "initialConcentration":"0",
        "name":"!Name"
    }
    if attribs["{%s}"%fbc+"charge"] == "":
        attribs["{%s}"%fbc+"charge"] = "0"
    metaid = genID()
    metabolite = etree.SubElement(species_tree,"species",metaid=metaid,attrib=attribs)
    notes_body = etree.SubElement(etree.SubElement(metabolite,"notes"),"{%s}"%xhtml+"body")
    for i in [key for key in list(val.keys()) if "!Identifier" not in key]:
        if val[i]!="":
            if key=="!Charge" and val[i]=="":
                val[i] == "0"
            etree.SubElement(notes_body,"{%s}"%xhtml+"p").text=i.replace("!","").replace("Notes:","").upper() + ": " + val[i]
    if any([val[i] for i in ["!Identifiers:chebi","!Identifiers:pubmed","!Identifiers:doi","!Identifiers:eco"] if val[i] != ""]):
        annotation_tree = etree.SubElement(etree.SubElement(etree.SubElement(metabolite,"annotation"),"{%s}"%rdf+"RDF"),"{%s}"%rdf+"Description",attrib={"{%s}"%rdf+"about":"#"+metaid})
        next_level = etree.SubElement(etree.SubElement(annotation_tree,"{%s}"%bqbiol+"is"),"{%s}"%rdf+"Bag")
        annotation_links={
            "!Identifiers:chebi":"http://identifiers.org/",
            "!Identifiers:pubmed":"http://identifiers.org/pubmed/",
            "!Identifiers:doi":"http://identifiers.org/doi/",
            "!Identifiers:eco":"http://www.evidenceontology.org/term/"
        }
        for i in ["!Identifiers:chebi","!Identifiers:pubmed","!Identifiers:doi","!Identifiers:eco"]:
            if val[i]!="":
                if i == "!Identifiers:pubmed":
                    etree.SubElement(next_level,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":"https://identifiers.org/pubchem.compound/"+val[i]})
                else:
                    etree.SubElement(next_level,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":annotation_links[i]+val[i]})

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

def genHead(parent,booltype):
    #function to generate the and/or xml field
    if booltype == "or":
        branch = etree.SubElement(parent,"{%s}"%fbc+"or",attrib={"sboTerm":"SBO:0000174"})
    else:
        branch = etree.SubElement(parent,"{%s}"%fbc+"and",attrib={"sboTerm":"SBO:0000173"})
    return branch

def parse(parent,my_list):
    if my_list == []: #handle empty gene associations
        result =  None
        return None
    while type(my_list) == list and len(my_list) == 1: #whilst there is a single entry in the list, unpack it
        my_list = my_list[0]
    if type(my_list) == str: #Handling single genes
        result = ("single",my_list)
    else:
        if any(type(i) == list for i in my_list): #If there are lists (nested Gene associations)
            for index,item in enumerate(my_list):
                #unpack
                if type(item) == list and len(item) == 1:
                    my_list[index] = item[0]
            types = None
            op_type = my_list[1]
            types = op_type
            while op_type in my_list:
                my_list.remove(op_type)
            for index,item in enumerate(my_list): #start diving down levels
                if type(item)==list:
                    op_type=item[1]
                    while op_type in item:
                        item.remove(op_type)
                    for index2,item2 in enumerate(item):
                        if type(item2)==list:
                            op_type2=item2[1]
                            while op_type2 in item2:
                                item2.remove(op_type2)
                            item[index2]=(op_type2,item2)
                    my_list[index] = (op_type,item)
            result = (types,my_list)
        else:
            op_type = my_list[1]
            while op_type in my_list:
                my_list.remove(op_type)
            result = (op_type,my_list)
    #create the xml tree
    gpr = etree.SubElement(parent,"{%s}"%fbc+"GeneProductAssociation")
    #simple case
    if result[0] == 'single':
        etree.SubElement(gpr,"{%s}"%fbc+"geneProductRef",attrib={"{%s}"%fbc+"geneProduct":"G_"+result[1]})
    #No nesting bool
    elif all(type(i) != tuple for i in result[1]):
        branch = genHead(gpr,result[0])
        for i in result[1]:
            etree.SubElement(branch,"{%s}"%fbc+"geneProductRef",attrib={"{%s}"%fbc+"geneProduct":"G_"+i})
    #complex case
    else:
        branch = genHead(gpr,result[0])
        for i in result[1]: #level diving
            if type(i) == tuple:
                inner = genHead(branch,i[0])
                for j in i[1]:
                    if type(j) == tuple:
                        inner2 = genHead(branch,j[0])
                        for k in j[1]:
                            etree.SubElement(inner2,"{%s}"%fbc+"geneProductRef",attrib={"{%s}"%fbc+"geneProduct":"G_"+k})
                    else:
                        etree.SubElement(inner,"{%s}"%fbc+"geneProductRef",attrib={"{%s}"%fbc+"geneProduct":"G_"+j})

            else:
                etree.SubElement(branch,"{%s}"%fbc+"geneProductRef",attrib={"{%s}"%fbc+"geneProduct":"G_"+i[1]})
    return gpr

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

# IDs !Identifiers:kegg.reaction	!Identifiers:rheadb_exact	!Identifiers:rheadb_fuzzy	!Identifiers:pubmed	!Identifiers:doi	!Identifiers:eco
# Other !Reaction	!Name	!ReactionFormula	!IsReversible	!GeneAssociation	!Pathway	!SuperPathway	!Comment	!Curator	!Notes:EC NUMBER	!Notes:AUTHORS
ignore = ["!Identifiers:kegg.reaction","!Identifiers:rheadb_exact","!Identifiers:rheadb_fuzzy","!Identifiers:pubmed","!Identifiers:doi","!Identifiers:eco",
"!Authors","!ReactionFormula","!SuperPathway","!Name","!IsReversible"]

for key,val in compiler.tables.get("Reaction").data.items():
    metaid = genID()
    attribs = {
        "fast":"false",
        "reversible":val["!IsReversible"].lower(),
        "metaid":metaid,
        "id":key,
        "name":val["!Name"],
        "{%s}"%fbc+"upperFluxBound":"UPPER_BOUND"
    }
    if attribs["reversible"] == "true":
        attribs["{%s}"%fbc+"lowerFluxBound"] = "LOWER_BOUND"
    else:
        attribs["{%s}"%fbc+"lowerFluxBound"] = "ZERO_BOUND"
    reaction_field = etree.SubElement(reaction_tree,"reaction",attrib=attribs)
    notes_body = etree.SubElement(etree.SubElement(reaction_field,"notes"),"{%s}"%xhtml+"body")
    for i in [key2 for key2 in list(val.keys()) if key2 not in ignore]:
        if val[i]!="":
            etree.SubElement(notes_body,"{%s}"%xhtml+"p").text=i.replace("!","").replace("Notes:","").replace("Pathway","Subsystem").upper() + ": " + val[i]

    annotation_links={
        "!Identifiers:kegg.reaction":"http://identifiers.org/kegg:",
        "!Identifiers:pubmed":"http://identifiers.org/pubmed/",
        "!Identifiers:doi":"http://identifiers.org/doi/",
        "!Identifiers:eco":"http://www.evidenceontology.org/term/",
        "!Identifiers:rheadb_exact":"http://identifiers.org//reaction?id="
    }
    if any([val[i] for i in annotation_links if val[i] != ""]):
        annotation_tree = etree.SubElement(etree.SubElement(etree.SubElement(reaction_field,"annotation"),"{%s}"%rdf+"RDF"),"{%s}"%rdf+"Description",attrib={"{%s}"%rdf+"about":"#"+metaid})
        next_level = etree.SubElement(etree.SubElement(annotation_tree,"{%s}"%bqbiol+"is"),"{%s}"%rdf+"Bag")

        for i in list(annotation_links.keys()):
            if val[i]!="":
                for j in val[i].replace(" ","").split(";"):
                    etree.SubElement(next_level,"{%s}"%rdf+"li",attrib={"{%s}"%rdf+"resource":annotation_links[i]+j})

    
    genes = "("+val["!GeneAssociation"]+")"
    parens = pyparsing.nestedExpr( '(', ')', content=pyparsing.Word(pyparsing.alphanums) | ' or ' | " and " )
    r = parens.parseString(genes)[0].asList()
    er = deepcopy(r)
    try:
        parse(reaction_field,r)
    except Exception as e:
        print(key,er)
        print(e)
    
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

output_model.write(etree.tostring(sbml,encoding="UTF-8",standalone=False,xml_declaration=True,pretty_print=True))
output_model.close()





#######################################################################################################################
## pretty print fragment
# with open(OUTPUT_NAME,"rb") as f:
#     parser = etree.XMLParser(remove_blank_text=True)
#     tree = etree.parse(f, parser)
#     print(etree.tostring(root,encoding="UTF-8",standalone=False,xml_declaration=True,pretty_print=True).decode())
