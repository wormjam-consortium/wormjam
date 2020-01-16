# load complete model
read_sbtab(model_folder)

# get all metabolites used in reactions ----------------------------------------
# metabolite_reaction <- `Reaction-SBtab.tsv_table`$`!ReactionFormula` %>% 
#   map(.f=~str_extract_all(.x, "M_\\w+_(c|m|e|n)")) %>% 
#   unlist() %>% unique()

metabolite_reaction <- `Reaction-SBtab.tsv_table`$`!ReactionFormula` %>% 
  str_extract_all("M_\\w+_(c|m|e|n)") %>% 
  unlist() %>% unique()

# get all metabolites in compound table
metabolite_compound <- `Compound-SBtab.tsv_table`$`!ID`
  
# perform qc on metabolites ----------------------------------------------------
# get metabolites not used in reactions
metabolites_not_used <- metabolite_compound[!metabolite_compound %in% metabolite_reaction]

# get metabolites missing in compound table
metabolites_missing <- metabolite_reaction[!metabolite_reaction %in% metabolite_compound]


`Compound-SBtab.tsv_table_not_used` <- `Compound-SBtab.tsv_table` %>%
  filter(`!ID` %in% metabolites_not_used)

`Compound-SBtab.tsv_table_missing` <- `Compound-SBtab.tsv_table` %>% 
  filter(`!ID` %in% metabolites_missing)

`Compound-SBtab.tsv_table_missing` <- add_row(`Compound-SBtab.tsv_table_missing`, `!ID` = metabolites_missing)

# write qc results
write_metabolite_qc_sbtab(model_folder)
