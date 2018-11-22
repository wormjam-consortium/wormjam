package org.sbml.wormjam;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.sbml.jsbml.Species;

import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;

/**
 * 
 * @author rodrigue
 *
 */
public class WormBaseCompoundTableImprovement {

  private static final String NOTES_CHEBI_NEUTRAL = "Notes:ChEBI_neutral";
  private static final String NOTES_INCHI_KEY_NEUTRAL = "Notes:InChIKey_neutral";
  private static final String NOTES_INCHI_NEUTRAL = "Notes:InChI_neutral";
  private static final String NOTES_FORMULA_NEUTRAL = "Notes:FORMULA_Neutral";
  private static final String NOTES_INCHI_KEY = "Notes:InChIKey";
  private static final String NOTES_INCHI = "Notes:InChI";
  private static final String NOTES_FORMULA = "Notes:FORMULA";
  private static final String CURATOR = "Curator";
  private static final String COMMENT = "Comment";
  private static final String CHARGE = "Charge";
  private static final String IDENTIFIERS_ECO = "Identifiers:eco";
  private static final String IDENTIFIERS_DOI = "Identifiers:doi";
  private static final String IDENTIFIERS_PUBMED = "Identifiers:pubmed";
  private static final String IDENTIFIERS_KEGG_COMPOUND = "Identifiers:kegg.compound";
  private static final String IDENTIFIERS_CHEBI = "Identifiers:chebi";

  // Create client
  private static final ChebiWebServiceClient chebiWSClient = new ChebiWebServiceClient();

  
  /**
   * @param args the arguments
   */
  public static void main(String[] args) {

    Map<String, Species> wormSpeciesAnnotations = new HashMap<String, Species>();
    List<String> wormSpeciesAnnotationIdList = new ArrayList<String>();
    Map<String, Species> wormSpeciesCurrent = new TreeMap<String, Species>();
    List<String> wormSpeciesCurrentIdList = new ArrayList<String>();
    Map<String, Entity> chebiMap = new HashMap<String, Entity>();
    Set<String> chebiIdSet = new HashSet<String>(); 
    
    // Reading Michael compounds table
    String geneAnnotationTableFileName = args[0];
    
    try{
      // Open the file
      InputStream fstream = new FileInputStream(geneAnnotationTableFileName);

      
      // File content:
      // 0 !ID    !Name   !Location   !Charge !Identifiers:chebi  !Identifiers:kegg.compound  !Identifiers:pubmed 
      // 7 !Identifiers:doi    !Identifiers:eco    !Comment    !Curator    !Notes:FORMULA  !Notes:InChI    !Notes:InChIKey
      // 14 !Notes:FORMULA_Neutral  !Notes:InChI_neutral    !Notes:InChIKey_neutral !Notes:ChEBI_neutral
      
      // M_lipoate_e    (R)-lipoate extracellular   -1  CHEBI:30313 C16241              formula corrected to charged version    wimi    
      // C8H13O2S2   "InChI=1S/C8H14O2S2/c9-8(10)4-2-1-3-7-5-6-11-12-7/h7H,1-6H2,(H,9,10)/p-1"   AGBQKNBQESQNJD-UHFFFAOYSA-M C8H14O2S2   
      // "InChI=1S/C8H14O2S2/c9-8(10)4-2-1-3-7-5-6-11-12-7/h7H,1-6H2,(H,9,10)/t7-/m1/s1" AGBQKNBQESQNJD-SSDOTTSWSA-N CHEBI:30314
      // 
      //
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        // System.out.println (strLine);
        
        // removing the header line
        if (strLine.startsWith("#") || strLine.startsWith("!") || strLine.trim().length() == 0) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 4) {
        	
        	Species s = new Species(2, 1);
          String id = getToken(tokens[0]);
          
          s.putUserObject("id", id); // s.setId(id);
          s.setName(getToken(tokens[1]));
          s.setCompartment(getToken(tokens[2]));
          
          s.putUserObject(CHARGE, tokens[3]);
          
          if (tokens.length >= 5) {
            String chebiId = getToken(tokens[4]);
            
            if (chebiId.length() > 0 && !chebiId.contains("CHEBI")) {
              System.out.println("Problem11 with id '" + chebiId + "'");
            }
            if (chebiId.length() > 0) {
              s.putUserObject(IDENTIFIERS_CHEBI, chebiId); 
              chebiIdSet.add(chebiId);
            }
          }
          if (tokens.length >= 6) { s.putUserObject(IDENTIFIERS_KEGG_COMPOUND, getToken(tokens[5])); }
          if (tokens.length >= 7) { s.putUserObject(IDENTIFIERS_PUBMED, getToken(tokens[6])); }

          if (tokens.length >= 8) { s.putUserObject(IDENTIFIERS_DOI, getToken(tokens[7])); }          
          if (tokens.length >= 9) { s.putUserObject(IDENTIFIERS_ECO, getToken(tokens[8])); }
          if (tokens.length >= 10) { s.putUserObject(COMMENT, getToken(tokens[9])); }
          if (tokens.length >= 11) { s.putUserObject(CURATOR, getToken(tokens[10])); }
          if (tokens.length >= 12) { s.putUserObject(NOTES_FORMULA, getToken(tokens[11])); }
          if (tokens.length >= 13) { s.putUserObject(NOTES_INCHI, getToken(tokens[12])); }
          if (tokens.length >= 14) { s.putUserObject(NOTES_INCHI_KEY, getToken(tokens[13])); }
          
          if (tokens.length >= 15) { s.putUserObject(NOTES_FORMULA_NEUTRAL, getToken(tokens[14])); }
          if (tokens.length >= 16) { s.putUserObject(NOTES_INCHI_NEUTRAL, getToken(tokens[15])); }
          if (tokens.length >= 17) { s.putUserObject(NOTES_INCHI_KEY_NEUTRAL, getToken(tokens[16])); }
          if (tokens.length >= 18) { s.putUserObject(NOTES_CHEBI_NEUTRAL, getToken(tokens[17])); }

          wormSpeciesAnnotations.put(id, s);
          wormSpeciesAnnotationIdList.add(id);
          
        } else {
          System.out.println("Process compounds annotations - problem found " + tokens.length + " tokens.\n" + strLine);
        }
      }
      
      System.out.println("Read annotations for " + wormSpeciesAnnotations.size() + " entities");
      
      
      // System.out.println("\n\n" + wormGeneAnnotations.keySet());
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }

    // read the compound SBtab.tsv
    String geneTableFileName = args[1];
    
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(geneTableFileName);

      
      // File content:
      // 0 !ID    !Name   !Location   !Charge !Identifiers:chebi  !Identifiers:kegg.compound  !Identifiers:pubmed 
      // 7 !Identifiers:doi    !Identifiers:eco    !Comment    !Curator    !Notes:FORMULA  !Notes:InChI
      
      // M_lipoate_e    (R)-lipoate extracellular   -1  CHEBI:30313 C16241              formula corrected to charged version    wimi    
      // C8H13O2S2   "InChI=1S/C8H14O2S2/c9-8(10)4-2-1-3-7-5-6-11-12-7/h7H,1-6H2,(H,9,10)/p-1"
      // 
      //
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        // System.out.println (strLine);
        
          // removing the header line
          if (strLine.startsWith("#") || strLine.startsWith("!") || strLine.trim().length() == 0) {
            continue;
          }
          
          String[] tokens = strLine.split("\t");
          
          if (tokens.length >= 4) {
          	
          	Species s = new Species(2, 1);
            String id = getToken(tokens[0]);
            
            s.putUserObject("id", id); // s.setId(id);
            s.setName(getToken(tokens[1]));
            s.setCompartment(getToken(tokens[2]));
            
            s.putUserObject(CHARGE, getToken(tokens[3]));
            
            if (tokens.length >= 5) {
              String chebiId = getToken(tokens[4]);
              
              if (chebiId.length() > 0 && !chebiId.contains("CHEBI")) {
                System.out.println("Problem with id " + chebiId);
              }
              if (chebiId.length() > 0) {
                s.putUserObject(IDENTIFIERS_CHEBI, chebiId); 
                chebiIdSet.add(chebiId);
              }
            }
            if (tokens.length >= 6) { s.putUserObject(IDENTIFIERS_KEGG_COMPOUND, getToken(tokens[5])); }
            if (tokens.length >= 7) { s.putUserObject(IDENTIFIERS_PUBMED, getToken(tokens[6])); }

            if (tokens.length >= 8) { s.putUserObject(IDENTIFIERS_DOI, getToken(tokens[7])); }          
            if (tokens.length >= 9) { s.putUserObject(IDENTIFIERS_ECO, getToken(tokens[8])); }
            if (tokens.length >= 10) { s.putUserObject(COMMENT, getToken(tokens[9])); }
            if (tokens.length >= 11) { s.putUserObject(CURATOR, getToken(tokens[10])); }
            if (tokens.length >= 12) { s.putUserObject(NOTES_FORMULA, getToken(tokens[11])); }
            if (tokens.length >= 13) { s.putUserObject(NOTES_INCHI, getToken(tokens[12])); }
            if (tokens.length >= 14) { s.putUserObject(NOTES_INCHI_KEY, getToken(tokens[13])); }
          
            wormSpeciesCurrent.put(id, s);
            wormSpeciesCurrentIdList.add(id);
            
        } else {
          System.out.println("Process Compounds - problem found " + tokens.length + " tokens.");
        }
      }
      
      System.out.println("Read annotations for " + wormSpeciesCurrent.size() + " current entities");
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }

    // getting all chebi entities beforehand 45 by 45
    ArrayList<String> chebiIdList = new ArrayList<String>();
    
    for (String chebiId : chebiIdSet) {
      if (chebiId != null && chebiId.trim().length() > 0) {
        chebiIdList.add(chebiId);
      }
      
      if (chebiIdList.size() > 45) {
        try {
          List<Entity> chebiEntityList = chebiWSClient.getCompleteEntityByList(chebiIdList);
          
          if (chebiEntityList.size() < chebiIdList.size()) {
            System.out.println(chebiEntityList.size() - chebiIdList.size() + " not found chebi id in:" + chebiIdList);
          }
          for (Entity chebiEntity : chebiEntityList) {
            chebiMap.put(chebiEntity.getChebiId(), chebiEntity);
          }
        } catch (ChebiWebServiceFault_Exception e) {
          e.printStackTrace();
        }
        chebiIdList.clear();
      }
    }
    // getting the last chebi entities
    if (chebiIdList.size() > 0) {
      try {
        List<Entity> chebiEntityList = chebiWSClient.getCompleteEntityByList(chebiIdList);
        
        if (chebiEntityList.size() < chebiIdList.size()) {
          System.out.println(chebiEntityList.size() - chebiIdList.size() + " not found chebi id in:" + chebiIdList);
        }
        for (Entity chebiEntity : chebiEntityList) {
          chebiMap.put(chebiEntity.getChebiId(), chebiEntity);
          // System.out.println("Chebi Entity id = '" + chebiEntity.getChebiId() + "'");
          
        }
      } catch (ChebiWebServiceFault_Exception e) {
        e.printStackTrace();
      }
      chebiIdList.clear();
    }
    
    System.out.println("CHEBI Entity in the map = " + chebiMap.size() + " / " + chebiIdSet.size());

    // creating a list of not found chebi ids
    List<String> problematicChebiIds = new ArrayList<String>();
    for (String chebiId : chebiIdSet) {
      if (chebiMap.get(chebiId) == null) {
        problematicChebiIds.add(chebiId);
        System.out.println("Problem22 with '" + chebiId + "'");
      }
    }
    
    if (problematicChebiIds.size() > 0) {
      System.out.println("Problematic ids : " + problematicChebiIds);
    }
    
    int nbFounds = 0;
    int nbDiff = 0;
    int nbdiffName = 0;
    int nbdiffLocation = 0;
    int nbdiffCharge = 0;
    int nbChargeAdded = 0;
    int nbdiffChebi = 0;
    int nbChebiAdded = 0;
    int nbChebiDiffName = 0;
    int nbdiffFormula = 0;
    int nbFormulaAdded = 0;
    int nbDiffOther = 0;
    int nbAddedOther = 0;
    
    //  insert what we can find from MW
    try {
      // output file
      // 0 !ID    !Name   !Location   !Charge !Identifiers:chebi  !Identifiers:kegg.compound  !Identifiers:pubmed 
      // 7 !Identifiers:doi    !Identifiers:eco    !Comment    !Curator    !Notes:FORMULA  !Notes:InChI    !Notes:InChIKey
      // 14 !Notes:FORMULA_Neutral  !Notes:InChI_neutral    !Notes:InChIKey_neutral !Notes:ChEBI_neutral

      PrintWriter out = new PrintWriter(args[2]);
      out.println("!!SBtab SbtabVersion='1.0' TableType='Compound' TableName='C elegans metabolites'");
      out.println("!ID\t!Name\t!Location\t!Charge\t!Identifiers:chebi\t!Identifiers:kegg.compound\t!Identifiers:pubmed"
          + "\t!Identifiers:doi\t!Identifiers:eco\t!Comment\t!Curator\t!Notes:FORMULA\t!Notes:InChI\t!Notes:InChIKey"
          + "\t!Notes:FORMULA_Neutral\t!Notes:InChI_neutral\t!Notes:InChIKey_neutral\t!Notes:ChEBI_neutral");

      for (String id : wormSpeciesCurrentIdList) {
        if (wormSpeciesAnnotations.get(id) != null) {
          nbFounds++;
          Species currentS = wormSpeciesCurrent.get(id);
          Species newS = wormSpeciesAnnotations.get(id);
          boolean diff = false;
          
          String chebiId = (String) newS.getUserObject(IDENTIFIERS_CHEBI);
          Entity chebiEntity = chebiMap.get(chebiId);
          
          if (chebiEntity == null && chebiId != null && chebiId.trim().length() > 0) {
            System.out.println("CHEBI id '" + chebiId + "' not on the map !");
            chebiEntity = getCompleteChebiEntity((String) newS.getUserObject(IDENTIFIERS_CHEBI));

            if (chebiEntity != null) {
              chebiMap.put(chebiId, chebiEntity);
            }
          }
          
          // name
          if (!currentS.getName().equals(newS.getName())) {
            nbdiffName++;
            diff = true;
            
            // check with the value in chebi
            if (chebiEntity != null && !newS.getName().equalsIgnoreCase(chebiEntity.getChebiAsciiName())) {
              // System.out.println("'" + chebiId + "' name is different from chebi: here '" + newS.getName() + "' versus '" + chebiEntity.getChebiAsciiName() + "' in CHEBI.");
              nbChebiDiffName++;
            }
          }

          // check with the value in chebi
          if (chebiEntity != null && !newS.getName().equalsIgnoreCase(chebiEntity.getChebiAsciiName())) {
            
            // TODO - We update the name in newS if chebi id is different from chebi_Neutral
            
            // System.out.println("'" + chebiId + "' name is different from chebi: here '" + newS.getName() + "' versus '" + chebiEntity.getChebiAsciiName() + "' in CHEBI.");
            
          }

          
          
          // location / compartment
          if (!currentS.getCompartment().equals(newS.getCompartment())) {
            nbdiffLocation++;
            diff = true;
          }
          
          // charge
          if (currentS.getUserObject(CHARGE) == null && newS.getUserObject(CHARGE) != null) {
            nbChargeAdded++;
            diff = true;
          } else if (currentS.getUserObject(CHARGE) != null && (!currentS.getUserObject(CHARGE).equals(newS.getUserObject(CHARGE)))) {
            nbdiffCharge++;
            diff = true;
            
            // System.out.println(id + "\tCharge diff\t'" + currentS.getUserObject(CHARGE) + "'\t'" + newS.getUserObject(CHARGE) + "'");
            
            // check with the value in chebi
            if (chebiEntity != null && !newS.getUserObject(CHARGE).equals(chebiEntity.getCharge())) {
              // System.out.println("'" + chebiId + "' charge is different from chebi: here '" + newS.getUserObject(CHARGE) + "' versus '" + chebiEntity.getCharge() + "' in CHEBI.");
            }

          }

          // chebi
          if (currentS.getUserObject(IDENTIFIERS_CHEBI) == null && newS.getUserObject(IDENTIFIERS_CHEBI) != null) {
            nbChebiAdded++;
            diff = true;
          } else if (currentS.getUserObject(IDENTIFIERS_CHEBI) != null && (!currentS.getUserObject(IDENTIFIERS_CHEBI).equals(newS.getUserObject(IDENTIFIERS_CHEBI)))) {
            nbdiffChebi++;
            diff = true;
            // System.out.println(id + "\tChEBI diff\t'" + currentS.getUserObject(IDENTIFIERS_CHEBI) + "'\t'" + newS.getUserObject(IDENTIFIERS_CHEBI) + "'");
          }

          // kegg.compound
          String keggCurrent = (String) currentS.getUserObject(IDENTIFIERS_KEGG_COMPOUND);
          
          if (keggCurrent == null && newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND) != null && ((String) newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND)).trim().length() > 0) {
            nbAddedOther++;
            diff = true;
          } else if (keggCurrent != null && (!keggCurrent.equals(newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND)))) {
            if (keggCurrent.trim().length() > 0) {
              nbDiffOther++;
              // System.out.println(id + "\tKEGG.compound diff\t'" + currentS.getUserObject(IDENTIFIERS_KEGG_COMPOUND) + "'\t'" + newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND) + "'");
            }
            else {
              nbAddedOther++;
            }
            diff = true;
          }

          // pupmed
          if (currentS.getUserObject(IDENTIFIERS_PUBMED) == null && newS.getUserObject(IDENTIFIERS_PUBMED) != null && ((String) newS.getUserObject(IDENTIFIERS_PUBMED)).trim().length() > 0) {
            nbAddedOther++;
            diff = true;
          } else if (currentS.getUserObject(IDENTIFIERS_PUBMED) != null && (!currentS.getUserObject(IDENTIFIERS_PUBMED).equals(newS.getUserObject(IDENTIFIERS_PUBMED)))) {
            nbDiffOther++;
            diff = true;
            System.out.println(id + "\tpubmed diff\t" + currentS.getUserObject(IDENTIFIERS_PUBMED) + "\t" + newS.getUserObject(IDENTIFIERS_PUBMED));
          }
          
          // doi
          if (currentS.getUserObject(IDENTIFIERS_DOI) == null && newS.getUserObject(IDENTIFIERS_DOI) != null && ((String) newS.getUserObject(IDENTIFIERS_DOI)).trim().length() > 0) {
            nbAddedOther++;
            diff = true;
          } else if (currentS.getUserObject(IDENTIFIERS_DOI) != null && (!currentS.getUserObject(IDENTIFIERS_DOI).equals(newS.getUserObject(IDENTIFIERS_DOI)))) {
            nbDiffOther++;
            diff = true;
            System.out.println(id + "\tdoi diff\t'" + currentS.getUserObject(IDENTIFIERS_DOI) + "'\t'" + newS.getUserObject(IDENTIFIERS_DOI) + "'");
          }
          
          // eco
          String ecoCurrent =  (String) currentS.getUserObject(IDENTIFIERS_ECO);
          String ecoNew = (String) newS.getUserObject(IDENTIFIERS_ECO);
          
          if (ecoCurrent == null && ecoNew != null && ecoNew.trim().length() > 0) {
            nbAddedOther++;
            diff = true;
          } else if (ecoCurrent != null && (!ecoCurrent.equals(ecoNew))) {
            if (ecoCurrent.trim().length() > 0) {
              nbDiffOther++;
              System.out.println(id + "\teco diff\t'" + ecoCurrent + "'\t'" + ecoNew + "'");
            } else {
              nbAddedOther++;
            }
            diff = true;
          }
          
          // Comment
          String currentComment = (String) currentS.getUserObject(COMMENT);
          String newComment = (String) newS.getUserObject(COMMENT);
          
          if (newComment != null) {
            newComment = newComment.replaceAll("\"\"", "'");
            newS.putUserObject(COMMENT, newComment);
          }
          
          // check that the comment is contained in the newS comment
          if (newComment != null && currentComment != null && !newComment.contains(currentComment)) {
            System.out.println("Comment (" + id + "): '" + currentComment + "' ---> '" + newComment + "'."); 
            
            // do not merge comments, they have been merged by hand if needed
            // newS.putUserObject(COMMENT, currentComment + ". " + newComment);
          }
          
          // Curator
          String currentCurator = (String) currentS.getUserObject(CURATOR);
          String newCurator = (String) newS.getUserObject(CURATOR);

          // check that the curator is contained in the newS curator
          if (newComment != null && currentCurator != null && !newCurator.contains(currentCurator)) {
            // System.out.println("Curator: '" + currentCurator + "', '" + newCurator + "'.");
            newS.putUserObject(CURATOR, currentCurator + "," + newCurator);
          }
          
          // formula
          String newFormula = (String) newS.getUserObject(NOTES_FORMULA);
          
          if (currentS.getUserObject(NOTES_FORMULA) == null && newFormula != null) {
            nbFormulaAdded++;
            diff = true;
          } else if (currentS.getUserObject(NOTES_FORMULA) != null && (!currentS.getUserObject(NOTES_FORMULA).equals(newFormula))) {
            nbdiffFormula++;
            diff = true;
            
            if (nbdiffFormula < 25 || newFormula == null) {
              // System.out.println(id + "\tFormula diff\t" + currentS.getUserObject(NOTES_FORMULA) + "\t" + newFormula);
            }
            
            // check with the value in chebi
            String firstFormulaChebi = (chebiEntity != null && chebiEntity.getFormulae().size() > 0) ? chebiEntity.getFormulae().get(0).getData() : "";
            if (newFormula != null && !newFormula.equals(firstFormulaChebi)) {
              // System.out.println("'" + chebiId + "' formula is different from chebi: here '" + newFormula + "' versus '" + firstFormulaChebi + "' in CHEBI.");
            }
          }

          // inchi
          
          // TODO - check with CHEBI

          if (diff) {
            nbDiff++;
          }
          
          out.println(id + "\t" + newS.getName() + "\t" + newS.getCompartment() + "\t" + (newS.getUserObject(CHARGE) == null ? "" : newS.getUserObject(CHARGE)) 
              + "\t" + (newS.getUserObject(IDENTIFIERS_CHEBI) == null ? "" : newS.getUserObject(IDENTIFIERS_CHEBI)) + "\t" + (newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND) == null ? "" : newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND))
              + "\t" + (newS.getUserObject(IDENTIFIERS_PUBMED) == null ? "" : newS.getUserObject(IDENTIFIERS_PUBMED)) + "\t" + (newS.getUserObject(IDENTIFIERS_DOI) == null ? "" : newS.getUserObject(IDENTIFIERS_DOI))
              + "\t" + (newS.getUserObject(IDENTIFIERS_ECO) == null ? "" : newS.getUserObject(IDENTIFIERS_ECO)) + "\t" + (newS.getUserObject(COMMENT) == null ? "" : newS.getUserObject(COMMENT))
              + "\t" + (newS.getUserObject(CURATOR) == null ? "" : newS.getUserObject(CURATOR)) + "\t" + (newS.getUserObject(NOTES_FORMULA) == null ? "" : newS.getUserObject(NOTES_FORMULA))
              + "\t" + (newS.getUserObject(NOTES_INCHI) == null ? "" : newS.getUserObject(NOTES_INCHI)) + "\t" + (newS.getUserObject(NOTES_INCHI_KEY) == null ? "" : newS.getUserObject(NOTES_INCHI_KEY))
              + "\t" + (newS.getUserObject(NOTES_FORMULA_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_FORMULA_NEUTRAL)) + "\t" + (newS.getUserObject(NOTES_INCHI_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_INCHI_NEUTRAL))
              + "\t" + (newS.getUserObject(NOTES_INCHI_KEY_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_INCHI_KEY_NEUTRAL)) + "\t" + (newS.getUserObject(NOTES_CHEBI_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_CHEBI_NEUTRAL))
              );
          
        } else {
          System.out.println("Species '" + id + "' not present in MW list.");

          Species newS = wormSpeciesCurrent.get(id);
          
          out.println(id + "\t" + newS.getName() + "\t" + newS.getCompartment() + "\t" + (newS.getUserObject(CHARGE) == null ? "" : newS.getUserObject(CHARGE)) 
              + "\t" + (newS.getUserObject(IDENTIFIERS_CHEBI) == null ? "" : newS.getUserObject(IDENTIFIERS_CHEBI)) + "\t" + (newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND) == null ? "" : newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND))
              + "\t" + (newS.getUserObject(IDENTIFIERS_PUBMED) == null ? "" : newS.getUserObject(IDENTIFIERS_PUBMED)) + "\t" + (newS.getUserObject(IDENTIFIERS_DOI) == null ? "" : newS.getUserObject(IDENTIFIERS_DOI))
              + "\t" + (newS.getUserObject(IDENTIFIERS_ECO) == null ? "" : newS.getUserObject(IDENTIFIERS_ECO)) + "\t" + (newS.getUserObject(COMMENT) == null ? "" : newS.getUserObject(COMMENT))
              + "\t" + (newS.getUserObject(CURATOR) == null ? "" : newS.getUserObject(CURATOR)) + "\t" + (newS.getUserObject(NOTES_FORMULA) == null ? "" : newS.getUserObject(NOTES_FORMULA))
              + "\t" + (newS.getUserObject(NOTES_INCHI) == null ? "" : newS.getUserObject(NOTES_INCHI)) + "\t" + (newS.getUserObject(NOTES_INCHI_KEY) == null ? "" : newS.getUserObject(NOTES_INCHI_KEY))
              + "\t" + (newS.getUserObject(NOTES_FORMULA_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_FORMULA_NEUTRAL)) + "\t" + (newS.getUserObject(NOTES_INCHI_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_INCHI_NEUTRAL))
              + "\t" + (newS.getUserObject(NOTES_INCHI_KEY_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_INCHI_KEY_NEUTRAL)) + "\t" + (newS.getUserObject(NOTES_CHEBI_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_CHEBI_NEUTRAL))
              );

          
        }
      }
      

      // Writing the new Compound
      for (String id : wormSpeciesAnnotationIdList) {
        if (wormSpeciesCurrent.get(id) == null) {

          Species newS = wormSpeciesAnnotations.get(id);

          String compartment = newS.getCompartment();
          
          // adding the compartment if missing
          if (compartment == null || compartment.trim().length() == 0) {
            if (newS.getName().endsWith("c")) {
              compartment = "cytosol";
            } else if (newS.getName().endsWith("n")) {
              compartment = "nucleus";
            } else if (newS.getName().endsWith("m")) {
              compartment = "mitochondrion";
            } else if (newS.getName().endsWith("e")) {
              compartment = "extracellular";
            }  
          }
          
          // correct Schroeder name 
          String comment = (String) newS.getUserObject(COMMENT);
          
          if (comment != null && comment.trim().length() > 0) {
            if (comment.contains("Schr�der")) {
              comment = comment.replace("Schr�der", "Schroeder");
            }
          } else {
            comment = "";
          }
          
          out.println(id + "\t" + newS.getName() + "\t" + newS.getCompartment() + "\t" + (newS.getUserObject(CHARGE) == null ? "" : newS.getUserObject(CHARGE)) 
              + "\t" + (newS.getUserObject(IDENTIFIERS_CHEBI) == null ? "" : newS.getUserObject(IDENTIFIERS_CHEBI)) + "\t" + (newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND) == null ? "" : newS.getUserObject(IDENTIFIERS_KEGG_COMPOUND))
              + "\t" + (newS.getUserObject(IDENTIFIERS_PUBMED) == null ? "" : newS.getUserObject(IDENTIFIERS_PUBMED)) + "\t" + (newS.getUserObject(IDENTIFIERS_DOI) == null ? "" : newS.getUserObject(IDENTIFIERS_DOI))
              + "\t" + (newS.getUserObject(IDENTIFIERS_ECO) == null ? "" : newS.getUserObject(IDENTIFIERS_ECO)) + "\t" + comment
              + "\t" + (newS.getUserObject(CURATOR) == null ? "" : newS.getUserObject(CURATOR)) + "\t" + (newS.getUserObject(NOTES_FORMULA) == null ? "" : newS.getUserObject(NOTES_FORMULA))
              + "\t" + (newS.getUserObject(NOTES_INCHI) == null ? "" : newS.getUserObject(NOTES_INCHI)) + "\t" + (newS.getUserObject(NOTES_INCHI_KEY) == null ? "" : newS.getUserObject(NOTES_INCHI_KEY))
              + "\t" + (newS.getUserObject(NOTES_FORMULA_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_FORMULA_NEUTRAL)) + "\t" + (newS.getUserObject(NOTES_INCHI_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_INCHI_NEUTRAL))
              + "\t" + (newS.getUserObject(NOTES_INCHI_KEY_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_INCHI_KEY_NEUTRAL)) + "\t" + (newS.getUserObject(NOTES_CHEBI_NEUTRAL) == null ? "" : newS.getUserObject(NOTES_CHEBI_NEUTRAL))
              );
          
        }
      }
      
      out.flush();
      out.close();

    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }

    System.out.println("Found " + nbFounds + " / " + wormSpeciesCurrent.size() + ".");

//    int nbdiff = 0;
//    int nbdiffName = 0;
//    int nbdiffLocation = 0;
//    int nbdiffCharge = 0;
//    int nbChargeAdded = 0;
//    int nbdiffChebi = 0;
//    int nbChebiAdded = 0;
//    int nbdiffFormula = 0;
//    int nbFormulaAdded = 0;

    System.out.println("Differences: " + nbDiff + " - names: " + nbdiffName + " - location: " + nbdiffLocation + " - charge added: " + nbChargeAdded + ", modified: " + nbdiffCharge
        + " - chebi added: " + nbChebiAdded + ", modified: " + nbdiffChebi + " - formula added: " + nbFormulaAdded + ", modified: " + nbdiffFormula);
    System.out.println(nbChebiDiffName + " new names are different from the chebi name. " + nbDiffOther + " other diffs, " + nbAddedOther + " other additions/deletions (kegg and eco).");
  }

  /**
   * 
   * 
   * @param token
   * @return
   */
  private static String getToken(String token) {
    
    // removing surrounding "" if present
    if (token.startsWith("\"")) {
      token = token.substring(1, token.length() - 1);
    }
    
    return token.trim();
  }

  /**
   * 
   * 
   * @param chebiId
   * @return
   */
  public static Entity getCompleteChebiEntity(String chebiId) {
    
    if (chebiId == null || chebiId.trim().length() == 0) {
      return null;
    }
    
    try {
      // System.out.println("Invoking getCompleteEntity");

      Entity entity = chebiWSClient.getCompleteEntity(chebiId);
      // System.out.println("GetName: " + entity.getChebiAsciiName());

//      List<DataItem> synonyms = entity.getSynonyms();
//      for ( DataItem dataItem : synonyms ) { // List all synonyms
//        System.out.println("synonyms: " + dataItem.getData());
//      }

      return entity;

    } catch ( ChebiWebServiceFault_Exception e ) {
      System.err.println(e.getMessage());
    }

    return null;
  }
}
