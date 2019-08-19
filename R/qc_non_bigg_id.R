# load complete model
read_sbtab(model_folder)

# read current bigg table
bigg <- read_tsv("additional_dbs/bigg/2019-08-12/bigg_models_metabolites.txt")

# get universal ids from WormJam metabolite IDs and find metabolites not in BiGG
wormjam_meta_ids <- `Compound-SBtab.tsv_table` %>%
  select(`!ID`) %>%
  mutate(universal_id = stringr::str_remove_all(.$`!ID`, "M_"))

not_in_bigg <- wormjam_meta_ids %>% filter(!universal_id %in% bigg$bigg_id)

# create DF with metabolites not in bigg
`Compound-SBtab.tsv_table_not_in_bigg` <- `Compound-SBtab.tsv_table` %>% 
  filter(`!ID` %in% not_in_bigg$`!ID`)

# write qc results
write_metabolite_bigg_qc_sbtab(model_folder)