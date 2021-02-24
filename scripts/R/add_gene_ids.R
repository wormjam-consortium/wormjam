# load required libraries
library(tidyverse)
library(BridgeDbR)

# load required functions
source("R/wormjam_functions.R")

# load complete model
read_sbtab("model_versions/2019-07-09_draft/SBtab/tsv")

# create dir for BridgeDB
bridgeDb_dir <- paste0("additional_dbs/bridgedb/", Sys.Date())
dir.create(bridgeDb_dir)

# get current version of C elegans DB
dbLocation <- getDatabase("Caenorhabditis elegans",
                          location = bridgeDb_dir)

# create mapper
mapper <- loadDatabase(dbLocation)

# mapping to Uniprot/SwissProt/Trembl
map(mapper, "W", "WBGene00019008", "S")

# gene ontology mapper
map(mapper, "W", "WBGene00019008", "T")

# mapping from Uniprot to Rhea?
map(mapper, "S", "H1ZUV6", "E")


for(i in 1:nrow(`Gene-SBtab.tsv_table`)) {
  
  id <- `Gene-SBtab.tsv_table`$`!ID`[i]
  
  uniprot_ids <- map(mapper, "W", id, "S")
  ensemble <- map(mapper, "W", id, "EnCe")
  
  print(ensemble)
  
  # for(id in uniprot_ids) {
  #   
  #   enzymes <- map(mapper, "S", id, "E")
  #   
  #   if(length(enzymes) > 0) print(enzymes)
  # }
  
  #print(paste0(uniprot_ids, collapse = ";"))
}
