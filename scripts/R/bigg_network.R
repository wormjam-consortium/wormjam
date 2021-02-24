library(tidyverse)
library(magrittr)
library(tidygraph)
library(ggraph)

# read current bigg table
bigg_metabolites <- read_tsv("additional_dbs/bigg/2019-08-12/bigg_models_metabolites.txt")
bigg_reactions <- read_tsv("additional_dbs/bigg/2019-08-12/bigg_models_reactions.txt")

# get "substrate and products" -------------------------------------------------
bigg_reactions %<>% separate(reaction_string, into = c("substrates", "products"), sep = " <-> ", remove = FALSE)

# define hub metabolites -------------------------------------------------------
hub_metabolites <- c("h2o_c", "h2o_m", "h2o_n", "h2o_r", "h2o_x", "h2o_e",
                     "h_c", "h_m", "h_n", "h_r", "h_x", "h_e",
                     "pi_c", "pi_m", "pi_n", "pi_r", "pi_x", "pi_e",
                     "ppi_c", "ppi_m", "ppi_n", "ppi_r", "ppi_x", "ppi_e",
                     "atp_c", "atp_m", "atp_n", "atp_r", "atp_x", "atp_e",
                     "coa_c", "coa_m", "coa_n", "coa_r", "coa_x", "coa_e",
                     "nadph_c", "nadph_m", "nadph_n", "nadph_r", "nadph_x", "nadph_e",
                     "nad_c", "nad_m", "nad_n", "nad_r", "nad_x", "nad_e",
                     "adp_c", "adp_m", "adp_n", "adp_r", "adp_x", "adp_e",
                     "ACP_c", "ACP_m", "ACP_n", "ACP_r", "ACP_x", "ACP_e",
                     "co2_c", "co2_m", "co2_n", "co2_r", "co2_x", "co2_e",
                     "amp_c", "amp_m", "amp_n", "amp_r", "amp_x", "amp_e",
                     "nadp_c", "nadp_m", "nadp_n", "napd_r", "nadp_x", "nadp_e",
                     "pa_EC_c")

# isolate all metabolites and form pairs ---------------------------------------
metabolite_network <- tibble()

for(i in 1:nrow(bigg_reactions)) {
  
  substrate_metabolites <- unlist(str_extract_all(bigg_reactions$substrates[i], "\\w+_(c|m|e|n)"))
  
  if(length(substrate_metabolites) == 0) {
    substrate_metabolites <- NA
  }
  
  product_metabolites <- unlist(str_extract_all(bigg_reactions$products[i], "\\w+_(c|m|e|n)"))
  
  if(length(product_metabolites) == 0) {
    product_metabolites <- NA
  }
  
  if(!is.na(substrate_metabolites) | !is.na(product_metabolites)) {
    
    combinations <- crossing(substrate_metabolites, product_metabolites)
    names(combinations) <- c("from", "to")
    combinations$bigg_id <- bigg_reactions$bigg_id[i]
    
    metabolite_network <- bind_rows(metabolite_network, combinations)
  }
}

metabolites_network_filtered <- drop_na(metabolite_network)
metabolites_network_filtered %<>% filter(!from %in% hub_metabolites,
                                         !to %in% hub_metabolites) 

from_hub <- metabolites_network_filtered %>% count(from) %>% arrange(desc(n)) %>% filter(n > 100)
to_hub <- metabolites_network_filtered %>% count(to) %>% arrange(desc(n)) %>% filter(n > 100)
  
metabolites_network_filtered %<>% filter(!from %in% from_hub$from,
                                         !to %in% to_hub$to) 
  
# convert to network -----------------------------------------------------------
metabolite_network_graph <- as_tbl_graph(metabolites_network_filtered)

names <- metabolite_network_graph %>%
  activate(nodes) %>%
  pull(name)

from <- which(names == "ttdcea_c")
to <-  which(names == "hdca_c")

shortest <- metabolite_network_graph %>% 
  morph(to_shortest_path, from, to)

shortest <- shortest %>%
  mutate(selected_node = TRUE) %>%
  activate(edges) %>%
  mutate(selected_edge = TRUE) %>%
  unmorph()

shortest <- shortest %>%
  activate(nodes) %>%
  mutate(selected_node = ifelse(is.na(selected_node), 1, 2)) %>%
  activate(edges) %>%
  mutate(selected_edge = ifelse(is.na(selected_edge), 1, 2)) %>%
  arrange(selected_edge)

shortest_path_edges <- shortest %>%
  activate(edges) %>%
  filter(selected_edge == 2) %>%
  as_tibble()

shortest_path_nodes <- shortest %>%
  activate(nodes) %>%
  filter(selected_node == 2) %>%
  as_tibble()


bigg_reactions %>% filter(bigg_id %in% shortest_path_edges$bigg_id) %>% 
   select(bigg_id, name, reaction_string)

