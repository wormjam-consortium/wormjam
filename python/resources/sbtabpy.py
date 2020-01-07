#!/usr/bin/env python

"""SBTabReader.py - converts a csv with WormBase RNAi identifiers in the first column to a list of corresponding target genes

Requires (use pip to install):
openpyxl
"""
import os
import time
import tkinter as tk
import csv

__author__ = "Jake Hattwell"
__copyright__ = "None"
__credits__ = ["Jake Hattwell"]
__license__ = "CCO"
__version__ = "1"
__maintainer__ = "Jake Hattwell"
__email__ = "j.hattwell@uq.edu.au"
__status__ = "Live"


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
