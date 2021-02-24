# load required library
library(BridgeDbR)
library(tidyverse)

# load required functions
source("R/wormjam_functions.R")

# which model version shall be used
model_folder <- "curation"

# run all update and QC scripts
source("R/add_metabolite_ids.R")
source("R/qc_metabolites.R")
source("R/qc_non_bigg_id.R")