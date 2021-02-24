library(tidyverse)

# read current bigg table
bigg <- read_tsv("additional_dbs/bigg/2019-08-12/bigg_models_metabolites.txt")

# read WormJam IDs
worm_jam <- read_tsv(clipboard())

check_bigg <- function(x) {
  if(x %in% bigg$universal_bigg_id) {
    TRUE
  } else {
    FALSE
  }
}

worm_jam %>% rowwise() %>%  mutate(in_bigg = check_bigg(`Universal ID`)) %>% 
  write.table(file = "clipboard-16825", quote = FALSE, row.names = FALSE, col.names = FALSE, sep = "\t")


# make tidy version of BiGG metabolite table
bigg_tidy <- bigg %>% separate_rows(database_links, sep = ";")


# function to isolate correct ids
isolate_ids <- function(x) {
  
  id <- NA
  
  if(str_detect(x, "CHEBI")) {
    id <- unlist(stringr::str_extract(x, "CHEBI:\\d+$"))
  }
  
  return(id)
}

bigg_tidy %>% rowwise() %>%  mutate(id = isolate_ids(database_links))

