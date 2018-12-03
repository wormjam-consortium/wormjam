#!/usr/bin/env python

"""FullModelNetGraph.py - Generates an unoptimised network graph of all reactions in an SBtab reactions file for use with Gephi Network Editor

Requires (use pip to install):
matplotlib
networkx
openpyxl
"""


import os
import sys
import traceback

try:
    import matplotlib.pyplot as plt
    import networkx as nx
    import openpyxl
except (ImportError,ModuleNotFoundError) as e:
    print("This script relies on matplotlib, networkx and openpyxl. Please make sure that all 3 are installed")
    input("Press enter to exit...")
    quit()



__author__ = "Jake Hattwell"
__copyright__ = "None"
__credits__ = ["Jake Hattwell"]
__license__ = "CCO"
__version__ = "1"
__maintainer__ = "Jake Hattwell"
__email__ = "j.hattwell@uq.edu.au"
__status__ = "Prototype"

class dataset:
    
    def __init__(self,xlsx,headerRow=2):
        """[Create a dataset]
        
        Arguments:
            xlsx {[str]} -- [name of the excel formatted sbtab]
        
        Keyword Arguments:
            headerRow {int} -- [The row the header is in a=1,b=2...] (default: {2})
        """
        self.wb = openpyxl.load_workbook(xlsx)
        self.sheet = self.wb.active
        self.cols = self.sheet.max_column
        self.rows = self.sheet.max_row
        self.sbString = self.sheet.cell(row=1,column=1).value
        self.headerRow = headerRow
        self.headers = [self.sheet.cell(row=2,column = i).value for i in range(1,self.cols+1) if self.sheet.cell(row=self.headerRow,column = i)!= None]
        self.data = {str(self.sheet.cell(row=i,column = 1).value):{self.headers[j-1]:self.sheet.cell(row=i,column=j).value for j in range(1,self.cols+1)}for i in range(self.headerRow+1,self.rows+1)}
        self.freeze_panes = self.sheet.freeze_panes



class sparseReaction():
    def __init__(self,rxnname,rxn,reversibility,G):
        """A class that parses the reactions and adds them to the netowrk graph
        It is a class rather than a function because it was adapted from another script
        
        Arguments:
            rxnname {[str]} -- [The reaction name]
            rxn {[dict]} -- [Reaction from the imported SBtab]
            reversibility {[Boolean]} -- [Reversibility]
            G {[NetworkX Graph]} -- [The networkX graph that contains the network]
        """
        self.rxnname = rxnname
        #split the reaction into products and reactants
        r,p = rxn.split("<=>") #splits string into products and reactants
        #split products and reactants into lists of Stoich+Metabolites
        p,r = list(map(lambda x: [i.split(" ") for i in x.split("+")],[p,r]))
        #remove spaces from the products and reactants
        p,r = list(map(lambda x: list(map(lambda y: list(filter(lambda z: z != "",y)),x)),[p,r]))
        self.react,self.prod = {},{}

        for met in p:
            if len(met) != 0:
                try:
                    name = met.pop()
                except:
                    print(met)
            else:
                name = "EXTERIOR EXPORT"
            if name not in mets:
                G.add_node(name,ntype='Met',name=name)
            ## adding stoichiometry
            self.prod[name] = float(met[0]) if len(met) != 0 else 1
        for met in r:
            if len(met) != 0:
                try:
                    name = met.pop()
                except:
                    print(met)
            else:
                name = "EXTERIOR IMPORT"
            if name not in mets:
                G.add_node(name,ntype='Met',name=name)
            ## adding stoichiometry
            self.react[name] =  float(met[0]) if len(met) != 0 else 1

        G.add_node(self.rxnname,ntype='Rxn',name=self.rxnname)
        ##add reaction edges
        for key,val in self.react.items():
            G.add_edge(key, self.rxnname,weight=val)
        for key,val in self.prod.items():
            G.add_edge(self.rxnname,key,weight=val)
        #If reaction is reversible, add edges in the opposite direction
        if reversibility == 1:
            for key,val in self.react.items():
                G.add_edge(self.rxnname,key,weight=val)
            for key,val in self.prod.items():
                G.add_edge(key,self.rxnname,weight=val)


        


##storage container
mets = []

##graph to hold network
G = nx.DiGraph()
location = input("Path to excel formatted Reaction SBTab: ")
if os.path.isfile(location):
    d = dataset(location)
    print("Processing SBtab - probably a good time to get a coffee...")
    # creating datasets for all reactoins
    for key,val in d.data.items():
        try:
            r,n = val["!ReactionFormula"],key
            test = sparseReaction(n,r,val["!IsReversible"],G)
        except:
            print("Error opening",key)


    fig1 = plt.figure(1,figsize=(25,25))
    pos = nx.spring_layout(G, k=0.25, iterations=50)
    nx.draw(G, pos,font_size=8,with_labels=True,node_size=500, linewidths=0.25, font_weight='bold',  dpi=1000)
    location2 = input("File name to save graph as (don't include the extension): ")
    nx.write_gexf(G, location2+".gexf")
    print("Saved as",location2+".gexf")
else:
    print("Aborting. File not found at",location)
