# load required library
library(tidyverse)

# load required functions
source("R/wormjam_functions.R")

# which model version shall be used
model_folder <- "model_versions/2019-08-15_draft/SBtab/tsv"

# load complete model
read_sbtab(model_folder)

# replace ids in reactions
id_table <- read_tsv(clipboard())

for(i in 1:nrow(id_table)) {
  
  `Reaction-SBtab.tsv_table`$`!ReactionFormula` <- unlist(str_replace_all(`Reaction-SBtab.tsv_table`$`!ReactionFormula`, id_table$`!Notes:Old_ID`[i], id_table$`!ID`[i]))
  
}

# save changes to the files
write_sbtab(model_folder)
