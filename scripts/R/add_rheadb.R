# load required libraries
library(tidyverse)

# load required functions
source("R/wormjam_functions.R")

# load complete model
read_sbtab("model_versions/2019-08-15_draft/SBtab/tsv")

# read current Uniprot table
uniprot <- read_tsv("additional_dbs/uniprot/2019-06-26/20190626_uniprot.tab")

# filter Uniprot and split rows
uniprot_2 <-
  separate_rows(uniprot, `Gene names`, sep = " ") %>% 
  separate_rows(., `Rhea Ids`, sep = "; ")

# select only required columns
uniprot_select <- uniprot_2 %>% select(c(`Gene names`, `Rhea Ids`)) %>% 
  filter(!is.na(`Gene names`))

wormbase_select <- `Gene-SBtab.tsv_table` %>% select(c(`!ID`, `!Symbol`)) %>% 
  filter(!is.na(`!Symbol`))

# join table
joint_table <- left_join(wormbase_select, uniprot_select, by = c("!Symbol" = "Gene names")) %>% 
  filter(!is.na(`Rhea Ids`))


# iterature over reaction table and add RheaDB reactions
for(i in 1:nrow(`Reaction-SBtab.tsv_table`)) {
  
  # get WormBase Ids
  wormbase_ids <- unlist(stringr::str_extract_all(`Reaction-SBtab.tsv_table`$`!GeneAssociation`[i], "WBGene\\d+"))
  
  # check if results is there
  if(length(wormbase_ids) > 0) {
    
    # get the RheaDB IDs.
    rhea_ids <- joint_table %>%
      filter(`!ID` %in% wormbase_ids) %>% 
      select(c(`Rhea Ids`)) %>% 
      unlist() %>% 
      paste0(collapse = ";")

    # add RheaDB IDs to reaction table
    `Reaction-SBtab.tsv_table`$`!Identifiers:rheadb_exact`[i] <- rhea_ids
    
  }
}

# save changes to the files
write_sbtab("model_versions/2019-08-15_draft/SBtab/tsv")