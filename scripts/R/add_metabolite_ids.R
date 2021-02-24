# load the current metabolomics BridgeDB
mapper <- BridgeDbR::loadDatabase("D:/bridgedb/2019-09-04/metabolites_20190829.bridge")

# load complete model
read_sbtab(model_folder)

# iterate through all compounds in table
for(i in 1:nrow(`Compound-SBtab.tsv_table`)) {

  # neutral metabolites
  # get inchikey
  inchikey <- `Compound-SBtab.tsv_table`$`!Notes:InChIKey_neutral`[i]
  
  if(!is.na(inchikey)) {
    
    # perform mapping
    ids <- wormJam_mapper(inchikey, mapper)
    
    print(ids)
    
    # add ids to compound table
    # ChEBI are the primary IDs for WormJam and manually curated
    #`Compound-SBtab.tsv_table`$`!Notes:ChEBI_neutral`[i] <- ids[["ChEBI"]]
    `Compound-SBtab.tsv_table`$`!Notes:KEGG_neutral`[i] <- ids[["KEGG"]]
    `Compound-SBtab.tsv_table`$`!Notes:MetaCyc_neutral`[i] <- ids[["MetaCyc"]]
    `Compound-SBtab.tsv_table`$`!Notes:HMDB_neutral`[i] <- ids[["HMDB"]]
    `Compound-SBtab.tsv_table`$`!Notes:LipidMaps_neutral`[i] <- ids[["LipidMaps"]]
    `Compound-SBtab.tsv_table`$`!Notes:SwissLipids_neutral`[i] <- ids[["SwissLipids"]]
    `Compound-SBtab.tsv_table`$`!Notes:Wikidata_neutral`[i] <- ids[["Wikidata"]]
    `Compound-SBtab.tsv_table`$`!Notes:Pubchem_neutral`[i] <- ids[["PubChem"]]
    `Compound-SBtab.tsv_table`$`!Notes:Metabolights_neutral`[i] <- ids[["Metabolights"]]
    `Compound-SBtab.tsv_table`$`!Notes:Chemspider_neutral`[i] <- ids[["Chemspider"]]
  }
}

# save changes to the files
write_sbtab(model_folder)
