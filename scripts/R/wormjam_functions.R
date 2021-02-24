# load required libraries ======================================================
library(tidyverse)

# read all SBtab files =========================================================
read_sbtab <- function(folderPath) {
  
  print(paste0("read files in: ", folderPath))
  
  # get all files
  sbtab_files <- list.files(folderPath,
                            pattern = "-SBtab.tsv$",
                            full.names = TRUE)
  
  print(sbtab_files)
  
  # make new list
  sbtab_list <- list()
  
  # iterate over all files
  for(i in 1:length(sbtab_files)) {
    
    #get current file and add to list
    sbtab_file <- sbtab_files[i]
    sbtab_list[[i]] <- read_tsv(sbtab_file,
                                comment = "!!")
    
  }
  
  # correct names
  sbtab_names <- str_replace_all(basename(sbtab_files),
                                 "-SBTab.tsv", "")
  names(sbtab_list) <- paste0(sbtab_names,
                              "_table")
  
  # make tibble for each table
  for(i in 1:length(sbtab_list)) {
    
    assign(names(sbtab_list)[i],
           sbtab_list[[i]],
           envir = parent.frame())
    
  }
  
}

# write all SBTab tables to files ==============================================
write_sbtab <- function(folderPath) {
  
  # compartment table ----------------------------------------------------------
  # write header
  cat("!!SBtab SbtabVersion='1.0' TableType='Compartment' TableName='WormJam compartments'\n",
      file = paste0(folderPath, "/Compartment-SBtab.tsv"))
  
  # write content
  write.table(`Compartment-SBtab.tsv_table`,
              file=paste0(folderPath, "/Compartment-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Compound class table -------------------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Compound-class' TableName='Wormjam compound classes'\n",
      file=paste0(folderPath, "/Compound-class-SBtab.tsv"))
  
  # write content
  write.table(`Compound-class-SBtab.tsv_table`,
              file=paste0(folderPath, "/Compound-class-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Compound table -------------------------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Compound' TableName='C elegans metabolites'\n",
      file=paste0(folderPath, "/Compound-SBtab.tsv"))
  
  # write content
  write.table(`Compound-SBtab.tsv_table`,
              file=paste0(folderPath, "/Compound-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Curator table --------------------------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Curator' TableName='Example of Curator list'\n",
      file=paste0(folderPath, "/Curator-SBtab.tsv"))
  
  # write content
  write.table(`Curator-SBtab.tsv_table`,
              file=paste0(folderPath, "/Curator-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Defintion table ------------------------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Definition' TableName='Allowed_types'\n",
      file=paste0(folderPath, "/Definition-SBtab.tsv"))
  
  # write content
  write.table(`Definition-SBtab.tsv_table`,
              file=paste0(folderPath, "/Definition-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Gene table -----------------------------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Gene' TableName='C elegans genes'\n",
      file=paste0(folderPath, "/Gene-SBtab.tsv"))
  
  # write content
  write.table(`Gene-SBtab.tsv_table`,
              file=paste0(folderPath, "/Gene-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Pathway table --------------------------------------------------------------
  cat("!!SBtab SBtabVersion='1.0' TableType='Pathway' TableName='WormJam pathways'\n",
      file=paste0(folderPath, "/Pathway-SBtab.tsv"))
  
  # write content
  write.table(`Pathway-SBtab.tsv_table`,
              file=paste0(folderPath, "/Pathway-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Reaction table -------------------------------------------------------------
  cat("!!SBtab SBtabVersion='1.0' TableType='Reaction' TableName='Reaction'\n",
      file=paste0(folderPath, "/Reaction-SBtab.tsv"))
  
  # write content
  write.table(`Reaction-SBtab.tsv_table`,
              file=paste0(folderPath, "/Reaction-SBtab.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
}

# write all SBTab tables to files ==============================================
write_metabolite_qc_sbtab <- function(folderPath) {
  
  # check if qc folder exists
  if(!dir.exists(paste0(folderPath, "/qc/"))) {
    dir.create(paste0(folderPath, "/qc/"))
  }
  
  # Compound table compounds not used ------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Compound' TableName='C elegans metabolites'\n",
      file=paste0(folderPath, "/qc/Compound-SBtab_not_used.tsv"))
  
  # write content
  write.table(`Compound-SBtab.tsv_table_not_used`,
              file=paste0(folderPath, "/qc/Compound-SBtab_not_used.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  # Compound table compounds missing
  cat("!!SBtab SbtabVersion='1.0' TableType='Compound' TableName='C elegans metabolites'\n",
      file=paste0(folderPath, "/qc/Compound-SBtab_missing.tsv"))
  
  # write content
  write.table(`Compound-SBtab.tsv_table_missing`,
              file=paste0(folderPath, "/qc/Compound-SBtab_missing.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  
}

# write all SBTab tables to files ==============================================
write_metabolite_bigg_qc_sbtab <- function(folderPath) {
  
  # check if qc folder exists
  if(!dir.exists(paste0(folderPath, "/qc/"))) {
    dir.create(paste0(folderPath, "/qc/"))
  }
  
  # Compound table compounds not used ------------------------------------------
  cat("!!SBtab SbtabVersion='1.0' TableType='Compound' TableName='C elegans metabolites'\n",
      file=paste0(folderPath, "/qc/Compound-SBtab_not_in_bigg.tsv"))
  
  # write content
  write.table(`Compound-SBtab.tsv_table_not_in_bigg`,
              file=paste0(folderPath, "/qc/Compound-SBtab_not_in_bigg.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  
}


# write all SBTab tables to files ==============================================
write_reaction_charge_qc_sbtab <- function(folderPath) {
  
  # check if qc folder exists
  if(!dir.exists(paste0(folderPath, "/qc/"))) {
    dir.create(paste0(folderPath, "/qc/"))
  }
  
  # Compound table compounds not used ------------------------------------------
  cat("!!SBtab SBtabVersion='1.0' TableType='Reaction' TableName='Reaction'\n",
      file=paste0(folderPath, "/qc/Reaction-SBtab_charge_unbalanced.tsv"))
  
  # write content
  write.table(`Reaction-SBtab.tsv_charge_unbalanced`,
              file=paste0(folderPath, "/qc/Reaction-SBtab_charge_unbalanced.tsv"),
              sep = "\t",
              quote = FALSE,
              row.names = FALSE,
              na = "",
              append=TRUE)
  
  
}


# function to map WormJam InChI key to external DB id ==========================
wormJam_mapper <- function(inchikey, mapper) {
  
  # get all codes required in WormJam
  wormjam_codes <- .get_wormjam_codes()
  
  # generate empty list for ids
  ids <- list()
  
  for(wormjam_code in names(.get_wormjam_codes())) {
    
    wormjam_mapping <- BridgeDbR::map(mapper,
                                      inchikey,
                                      source = getSystemCode("InChIKey"),
                                      target = wormjam_codes[[wormjam_code]])
    
    if(length(wormjam_mapping) > 0) {
      
      # isolate based on regex
      if(wormjam_code == "ChEBI") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^CHEBI:\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "KEGG") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^C\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "MetaCyc") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^CPD-\\d{5}$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "HMDB") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^HMDB\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "LipidMaps") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^LM(FA|GL|GP|SP|ST|PR|SL|PK)[0-9]{4}([0-9a-zA-Z]{4,6})?$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "SwissLipids") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^SLM:\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "Wikidata") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^Q\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "PubChem") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "Metabolights") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^MTBLC\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } else if(wormjam_code == "Chemspider") {
        
        ids_list <- unlist(stringr::str_extract_all(wormjam_mapping,
                                                    "^\\d+$"))
        
        ids[[wormjam_code]] <- paste0(ids_list, collapse = ";")
        
      } 
      
      


    } else {
      
      ids[[wormjam_code]] <- NA
      
    }
  }
  
  return(ids)
  
}

# helper function for BridgDbR codes in WormJam ================================
.get_wormjam_codes <- function() {
  wormjam_codes <- list(
    "ChEBI" = BridgeDbR::getSystemCode("ChEBI"),
    "KEGG" = BridgeDbR::getSystemCode("KEGG Compound"),
    "MetaCyc" = BridgeDbR::getSystemCode("MetaCyc"),
    "HMDB" = BridgeDbR::getSystemCode("HMDB"),
    "LipidMaps" = BridgeDbR::getSystemCode("LIPID MAPS"),
    "SwissLipids" = BridgeDbR::getSystemCode("SwissLipids"),
    "Wikidata" = BridgeDbR::getSystemCode("Wikidata"),
    "PubChem" = BridgeDbR::getSystemCode("PubChem-compound"),
    "Metabolights" = BridgeDbR::getSystemCode("MetaboLights Compounds"),
    "Chemspider" = BridgeDbR::getSystemCode("Chemspider")
  )
  
  return(wormjam_codes)
}  
