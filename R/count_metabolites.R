# load required library
library(tidyverse)

# load required functions
source("R/wormjam_functions.R")

# load complete model
read_sbtab("model_versions/2019-08-01_draft/SBtab/tsv")

# count occurence of each metabolite in all reactions
metabolite_counts <- map(`Compound-SBtab.tsv_table`$`!ID`,
                         function(x) {
                           count <- stringr::str_count(`Reaction-SBtab.tsv_table`$`!ReactionFormula`,
                                                       x) %>% sum()
                                     return(paste0(x, ":", count))
                           }
                         ) %>% unlist() %>% as_tibble() %>% 
  separate(value, c("name", "count"), sep = ":")

metabolite_counts <- metabolite_counts %>% mutate(count = as.integer(count))
