#!/usr/bin/env python

"""WBRNAi2WBGene.py - converts a csv with WormBase RNAi identifiers in the first column to a list of corresponding target genes

Requires (use pip to install):
matplotlib
networkx
openpyxl
"""


import csv
import json
import urllib.request

__author__ = "Jake Hattwell"
__copyright__ = "None"
__credits__ = ["Jake Hattwell"]
__license__ = "CCO"
__version__ = "1"
__maintainer__ = "Jake Hattwell"
__email__ = "j.hattwell@uq.edu.au"
__status__ = "Live"

#read csv file
filename = input("CSV filename (e.g. data.csv): ")
with open(filename, newline='') as csvfile:
    data = list(csv.reader(csvfile, delimiter=',', quotechar='|'))

#grab first column
data = [i[0] for i in data]
results = {}
for i in data:
    #attempt to access WormBase API and find gene target
    #if it fails, place entry not found error in cell
    try:
        
        m = "/".join([str(data.index(i)+1),str(len(data))])
        print(m)
        link = "".join(["http://rest.wormbase.org/rest/widget/rnai/",i.replace('"',""),"/overview"])
        result = urllib.request.urlopen(link)
        result = result.read().decode('UTF-8')
        result = json.loads(result)
        results[i] = result['fields']['targets']['data'][0]['gene']['id']
    except:
        results[i] = i + "WormBase entry not found"

#saving results
exportFileName = input("File to save to (e.g. output.txt, output.log, output.tsv): ")
with open(exportFileName,'w+') as f:
    for key,val in results.items():
        f.write(key+"\t"+val+"\n")
