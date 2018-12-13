package org.sbml.wormjam;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.CVTerm.Qualifier;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.JSBML;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.GroupsConstants;
import org.sbml.jsbml.ext.groups.GroupsModelPlugin;
import org.sbml.jsbml.text.parser.CobraFormulaParser;
import org.sbml.jsbml.text.parser.ParseException;

/**
 * @author rodrigue
 *
 */
public class SBtab2SBML {

  
  
  
  private static final String EC_CODE_ID_SET = "EC_CODE_ID_SET";
  private static final String GO_PROCESS_ID_SET = "GO_PROCESS_ID_SET";
  private static final double LOWER_FLUX_BOUND = -1000;
  private static final double UPPER_FLUX_BOUND = 1000;
  
  private static final String IDENTIFIERS_ORG = "http://identifiers.org/";
  
  /**
   * 
   */
  private static final boolean ignoreDeleted = true;
  
  private static final boolean ignoreInvalidChemicalformula = false;

  private static Parameter lowerFluxBound = new Parameter();
  private static Parameter zeroFluxBound = new Parameter();
  private static Parameter upperFluxBound = new Parameter();
  private static LocalParameter fluxValue = new LocalParameter();
  
  private static Map<String, Creator> curatorMap = new HashMap<String, Creator>();
  private static Map<String, WGene> geneMap = new HashMap<String, WGene>();
  private static SortedSet<String> geneIDMissing = new TreeSet<String>();
  private static SortedSet<String> speciesReferenceMissing = new TreeSet<String>();
  private static Map<String, Group> groupPathwayMap = new HashMap<String, Group>();
  private static Set<String> unknownPathwaySet = new HashSet<String>();
  
  static {
    lowerFluxBound.setId("LOWER_BOUND");
    lowerFluxBound.setValue(LOWER_FLUX_BOUND);
    zeroFluxBound.setId("ZERO_BOUND");
    zeroFluxBound.setValue(0);
    upperFluxBound.setId("UPPER_BOUND");
    upperFluxBound.setValue(UPPER_FLUX_BOUND);
    fluxValue.setId("FLUX_VALUE");
    fluxValue.setValue(0);
  }
  
  /**
   * 
   * @param args
   */
  public static void main(String[] args) {
    
    // SBMLDocument doc = new SBMLDocument(2, 5);
    SBMLDocument doc = new SBMLDocument(3, 1);
    Model m = doc.createModel();
    
    List<File> files = new ArrayList<File>();
    
    // getting the list of files
    for (String arg : args) {
      if (arg.endsWith(".tsv") || arg.endsWith(".csv")) {
        files.add(new File(arg));
      } else if (arg.endsWith("_")) {
        // try to find sbtab files
        File compartment = new File(arg + "compartment.tsv");
        
        if (compartment.exists()) {
          files.add(compartment);
        }
        compartment = new File(arg + "compartment.csv");
        
        if (compartment.exists()) {
          files.add(compartment);
        }
        
        File compound = new File(arg + "compound.tsv");
        
        if (compound.exists()) {
          files.add(compound);
        }
        compound = new File(arg + "compound.csv");
        
        if (compound.exists()) {
          files.add(compound);
        }

        File reaction = new File(arg + "reaction.tsv");
        
        if (reaction.exists()) {
          files.add(reaction);
        }
        reaction = new File(arg + "reaction.csv");
        
        if (reaction.exists()) {
          files.add(reaction);
        }

        File curator = new File(arg + "curator.tsv");

        if (curator.exists()) {
          files.add(curator);
        }
        curator = new File(arg + "curator.csv");
        
        if (curator.exists()) {
          files.add(curator);
        }

        File pathway = new File(arg + "pathway.tsv");

        if (pathway.exists()) {
          files.add(pathway);
        }
        pathway = new File(arg + "pathway.csv");
        
        if (pathway.exists()) {
          files.add(pathway);
        }
        
        File gene = new File(arg + "gene.tsv");

        if (gene.exists()) {
          files.add(gene);
        }
        gene = new File(arg + "gene.csv");
        
        if (gene.exists()) {
          files.add(gene);
        }

      }
    }
    
    if (files.size() == 1) {
      // TODO - separate the SBtab that are into a single file.
    }

    if (files.size() < 2) {
      System.out.println("Usage: you need to pass at least 2 SBtab files.");
    }
    System.out.println("Starting conversion...");
    
    //
    // curators
    //
    for (File file : files) {
      if (file.getName().contains("Curator")) {
        processCurators(file, m);
      }
    }

    //
    // pathways
    //
    for (File file : files) {
      if (file.getName().contains("Pathway")) {
        processPathways(file, m);
      }
    }
    
    //
    // compartments
    //
    for (File file : files) {
      if (file.getName().contains("Compartment")) {
        processCompartments(file, m);
      }
    }
    if (m.getCompartmentCount() == 0) {
      m.createCompartment("default").setName("default compartment");
    }
    
    //
    // species
    //
    for (File file : files) {
      if (file.getName().contains("Compound")) {
        processSpecies(file, m);
      }
    }

    //
    // parameters
    //
    if (m.getLevel() == 3) {
      m.addParameter(lowerFluxBound);      
      m.addParameter(zeroFluxBound);
      m.addParameter(upperFluxBound);
      lowerFluxBound.setConstant(true);
      zeroFluxBound.setConstant(true);
      upperFluxBound.setConstant(true);
      
      FBCModelPlugin fbcM = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
      fbcM.setStrict(false);
    }
    
    //
    // genes
    //
    for (File file : files) {
      if (file.getName().contains("Gene")) {
        processGenes(file, m);
      }
    }
    
    //
    // reactions
    //
    for (File file : files) {
      if (file.getName().contains("Reaction")) {
        processReactions(file, m);
      }
    }

    try {
      if (args[args.length - 1].endsWith("xml")) {
        String fileName = args[args.length - 1];
        
        Date currentDate = Calendar.getInstance().getTime();
        m.getHistory().setCreatedDate(currentDate);
        m.getHistory().setModifiedDate(currentDate);
        new TidySBMLWriter().writeSBMLToFile(doc, fileName);
      } else {
        System.out.println(new TidySBMLWriter().writeSBMLToString(doc));
      }
    } catch (SBMLException e) {
      e.printStackTrace();
    } catch (XMLStreamException e) {
      e.printStackTrace();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
    
    
    System.out.println("Unknown genes: " + geneIDMissing);
    System.out.println("Unknown species in reaction formula: " + speciesReferenceMissing);
  }

  
  /**
   * 
   * @param file the genes SBtab file
   * @param m the {@link Model}
   */
  private static void processGenes(File file, Model m) {
   
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(file);

      // File content:
      // !ID !Identifiers:WormBase !Symbol !Locus !Name !GO_process !GO_function !GO_component !Identifiers:ec-code !Comment\t!Curator
      // 
      // index GO_process = 5, ec-code = 8 
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
        if (strLine.startsWith("#") || strLine.startsWith("!")) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 2) {
          
          WGene c = new WGene();
          
          geneMap.put(tokens[0], c);
          
          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            c.setSymbol(tokens[2].trim());
          }
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            c.setLocus(tokens[3].trim());
          }
          if (tokens.length > 4 && tokens[4].trim().length() > 0) {
            c.setName(tokens[4].trim());
          }
          if (tokens.length > 5 && tokens[5].trim().length() > 0) {
            String goProcesses = tokens[5].trim();
            
            if (goProcesses.length() > 0) {
              String[] goProcessIdArray = goProcesses.split(";");
              
              for (String goProcessId : goProcessIdArray) {
                c.addGoProcess(goProcessId);
              }
            }
          }
          if (tokens.length > 8 && tokens[8].trim().length() > 0) {
            String ecCodes = tokens[8].trim();
            
            if (ecCodes.length() > 0) {
              String[] ecCodeIdArray = ecCodes.split(";");
              
              for (String ecCodeId : ecCodeIdArray) {
                c.addEcCode(ecCodeId);
              }
            }
          }
          
        } else {
          System.out.println("Process genes - problem found " + tokens.length + " tokens.");
        }
      }
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }


  /**
   * 
   * @param file the curators file
   * @param m the {@link Model}
   */
  private static void processCurators(File file, Model m) {
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(file);

      // File content:
      // !ID     given-name      family-name     organization-name       email
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
        if (strLine.startsWith("#") || strLine.startsWith("!")) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 5) {
          
          Creator c = new Creator();
          
          curatorMap.put(tokens[0], c);
          
          if (tokens.length > 1 && tokens[1].trim().length() > 0) {
            c.setGivenName(tokens[1].trim());
          }
          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            c.setFamilyName(tokens[2].trim());
          }
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            c.setOrganisation(tokens[3].trim());
          }
          if (tokens.length > 4 && tokens[4].trim().length() > 0) {
            c.setEmail(tokens[4].trim());
          }
          
          m.getHistory().addCreator(c);
        } else {
          System.out.println("Process curators - problem found " + tokens.length + " tokens.");
        }
      }
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }


  /**
   * Process the compound SBtab file to add {@link Species} to the SBML document.
   * 
   * 
   * @param file the SBtab describing species
   * @param m the model
   */
  private static void processSpecies(File file, Model m) {
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(file);

      // File content:
      // !ID     !Name   !Location       !Charge !Formula        !Identifiers:chebi      !Identifiers:kegg.compound      !Identifiers:pubmed 
      // !Identifiers:doi        !Identifiers:eco        !Comment        !Curator       !Notes:InChI             !Notes:InChIKey      !Notes:FORMULA_Neutral  !Notes:InChI_neutral
      // !Notes:InChIKey_neutral !Notes:Name_neutral !Notes:BioCyc_neutral   !Notes:ChEBI_neutral    !Notes:HMDB_neutral !Notes:KEGG_neutral !Notes:LipidMaps_neutral    !Notes:ChEBI_Name_neutral
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String[] headers = null;
      String strLine;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        // System.out.println (strLine);
        
        // removing the header line
        if (strLine.startsWith("#") || strLine.startsWith("!")) {
          
          if (strLine.startsWith("!ID")) {
            // read header names
            headers = strLine.split("\t");
          }
          
          continue;
        }
        if ((strLine.contains("[DELETION]") || strLine.contains("[REMOVE]")) && ignoreDeleted) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 2) {
          
          Species s = m.createSpecies();
          s.initDefaults(2, 1, true);
          s.setInitialConcentration(0);
          
          try {
            s.setId(tokens[0].trim());
          } catch (IllegalArgumentException e) {
            System.out.println("Warning - problem with duplicated or invalid identifier: " + e.getMessage());
          } catch (Exception e) {
            System.out.println("Warning - problem with duplicated or invalid identifier: " + e.getMessage());            
          }
          
          s.setName(tokens[1].trim());
          
          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            // location
            String location = tokens[2].trim();
            
            if (m.getCompartment(location) != null) {
              s.setCompartment(location);
            } else {
              System.out.println("Warning - compartment '" + location + "' is not valid in this model.");
              s.setCompartment(m.getCompartment(0));
            }
          }          
          if (!s.isSetCompartment()) {
            s.setCompartment(m.getCompartment(0));
          }
          
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            String chargeStr = tokens[3].trim();
            Integer charge = null;
            
            try {
              charge = Integer.parseInt(chargeStr);
            } catch(NumberFormatException e) {
              System.out.println("Warning - Problem reading the charge: " + e.getMessage());
            }
            if (charge != null) {
              if (m.getLevel() == 2) {
                s.setCharge(charge);
              } else if (m.getLevel() == 3) {
                FBCSpeciesPlugin fbcS = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);
                fbcS.setCharge(charge);
              }
              // write it in the notes as well
              s.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>CHARGE: " + charge + "</p></body>");
            }
          }
          
          int index = 4;
          
          // !FORMULA
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>FORMULA: " + tokens[index].trim() + "</p></body>");
            
            if (m.getLevel() == 3) {
              FBCSpeciesPlugin fbcS = (FBCSpeciesPlugin) s.getPlugin(FBCConstants.shortLabel);
              
              try {
                if (!ignoreInvalidChemicalformula) {
                  fbcS.putUserObject(JSBML.ALLOW_INVALID_SBML, Boolean.TRUE); // trying to set invalid formula
                }
                
                fbcS.setChemicalFormula(tokens[index].trim());
              } catch (IllegalArgumentException e) {
                System.out.println("Species - invalid formula - '" + tokens[index].trim() + "'");
              }
            }
          }
          index++;
          
          // TODO - make the code more generic, using the header, so that it works whatever the order of the columns
          
          // !Identifiers:chebi  !Identifiers:kegg.compound  !Identifiers:pubmed !Identifiers:doi !Identifiers:eco    
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, IDENTIFIERS_ORG + "chebi/" + tokens[index].trim()));
          }
          index++;
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, IDENTIFIERS_ORG + "kegg.compound/" + tokens[index].trim()));
          }
          index++;
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IDENTIFIERS_ORG + "pubmed/" + tokens[index].trim()));
          }
          index++;
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IDENTIFIERS_ORG + "doi/" + tokens[index].trim()));
          }
          index++;
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IDENTIFIERS_ORG + "eco/" + tokens[index].trim()));
          }
          index++; // 10

          // !Comment    !Curator  
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>Comment: " + tokens[index].trim() + "</p></body>");
          }
          index++;
          if (tokens.length > index && tokens[index].trim().length() > 0) {
            s.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>Curator: " + tokens[index].trim() + "</p></body>");
          }
          index++;

          //
          // notes fields
          //
          for ( ; index < tokens.length ; index++) {
            
            if (tokens[index].trim().length() > 0) {
              String header = headers[index];
              header = header.substring(7);

              s.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>" + header + ": " + tokens[index].trim() + "</p></body>");
            }
          }
        } else {
          System.out.println("Process species - problem found " + tokens.length + " tokens.");
        }
      }
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Process the compartments SBtab file to add them to the SBML document.
   * 
   * 
   * @param file the SBtab describing compartments
   * @param m the model
   */
  private static void processCompartments(File file, Model m) {
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(file);

      // File content:
      // !Compartment   !Name   !Identifiers:go !Comment
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
        if (strLine.startsWith("#") || strLine.startsWith("!")) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 2) {
          
          Compartment c = m.createCompartment(tokens[0]);
          c.initDefaults(2, 1, true);
          c.setName(tokens[1]);
          c.setSize(1);
          
          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            c.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, IDENTIFIERS_ORG + "go/" + tokens[2].trim()));
          }
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            c.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>Comment: " + tokens[3].trim() + "</p></body>");
          }
          
        } else {
          System.out.println("Process compartments - problem found " + tokens.length + " tokens.");
        }
      }
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }
  
  /**
   * Process the reaction SBtab file to add them to the SBML document.
   * 
   * 
   * @param file the SBtab describing reactions
   * @param m the model
   */  
  private static void processReactions(File file, Model m) {
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(file);

      // File content:
      // !Reaction  !Name   !ReactionFormula    !IsReversible   !GeneAssociation    !Pathway    
      // !Identifiers:kegg.reaction  !Identifiers:pubmed !Identifiers:doi    !Identifiers:eco    
      // !Comment    !Curator    !Notes:EC NUMBER    !Notes:AUTHORS
      // 
      //
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        System.out.println (strLine);
        
        // removing the header line
        if (strLine.startsWith("#") || strLine.startsWith("!") || strLine.trim().length() == 0) {
          continue;
        }
        if ((strLine.contains("[DELETION]") || strLine.contains("[REMOVE]")) && ignoreDeleted) {
          continue;
        }

        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 2) {
          
          Reaction r = m.createReaction();
          r.initDefaults(2, 1, true);
          String geneAssociation = null;
          String subsystem = null;
          
          try {
            r.setId(tokens[0].trim().replace(' ', '_'));
          } catch (IllegalArgumentException e) {
            r.setId(tokens[0].trim().replace(' ', '_') + "_2");
            System.out.println("Warning - problem with duplicated or invalid identifier: " + e.getMessage());
          } catch (Exception e) {
            
            r.setId(tokens[0].trim().replace(' ', '_') + "_2");
            
            System.out.println("Warning - problem with duplicated or invalid identifier: " + e.getMessage());            
          }
          
          r.setName(tokens[1].trim());
          
          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            // location
            String reactionFormula = tokens[2].trim();
            
            parseReactionformula(r, reactionFormula);
          }          
          
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            String isReversibleStr = tokens[3].trim();
            Boolean reversible = null;
            
            reversible = Boolean.parseBoolean(isReversibleStr);
            r.setReversible(reversible);
          }
          
          // 4 !GeneAssociation    5 !Pathway 
          if (tokens.length > 4 && tokens[4].trim().length() > 0) {
            geneAssociation = tokens[4].trim();
            r.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>GENE_ASSOCIATION: " + geneAssociation + "</p></body>");
          }
          if (tokens.length > 5 && tokens[5].trim().length() > 0) {
            subsystem = tokens[5].trim();
            
            // process subsystem: rename if needed and add reaction to the proper group if L3
            String newSubsystem = processSubSystem(r, subsystem);
            
            r.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>SUBSYSTEM: " + newSubsystem + "</p></body>");
          }
          
          // !Identifiers:kegg.reaction  !Identifiers:pubmed !Identifiers:doi    !Identifiers:eco    
          if (tokens.length > 6 && tokens[6].trim().length() > 0) {
            r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, IDENTIFIERS_ORG + "kegg.reaction/" + tokens[6].trim()));
          }
          if (tokens.length > 7 && tokens[7].trim().length() > 0) {
            r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IDENTIFIERS_ORG + "pubmed/" + tokens[7].trim()));
          }
          if (tokens.length > 8 && tokens[8].trim().length() > 0) {
            r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IDENTIFIERS_ORG + "doi/" + tokens[8].trim()));
          }
          if (tokens.length > 9 && tokens[9].trim().length() > 0) {
            r.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_DESCRIBED_BY, IDENTIFIERS_ORG + "eco/" + tokens[9].trim()));
          }

          // !Comment    !Curator    !Notes:EC NUMBER    !Notes:AUTHORS
          if (tokens.length > 10 && tokens[10].trim().length() > 0) {
            String comment = tokens[10].trim().replace("<", "&lt;").replace(">", "&gt;");
            r.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>Comment: " + comment + "</p></body>");
          }
          if (tokens.length > 11 && tokens[11].trim().length() > 0) {
            r.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>Curator: " + tokens[11].trim() + "</p></body>");
          }
          if (tokens.length > 12 && tokens[12].trim().length() > 0) {
            r.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>EC-CODE: " + tokens[12].trim() + "</p></body>");
          }
          if (tokens.length > 13 && tokens[13].trim().length() > 0) {
            String authors = tokens[13].trim();
            if (!authors.equals("0")) {
              r.appendNotes("<body xmlns=\"" + JSBML.URI_XHTML_DEFINITION + "\"><p>AUTHORS: " + authors + "</p></body>");
            }
          }
          
          // getting the ec-code and goProcess annotations for the reaction
          if (geneAssociation != null && geneAssociation.trim().length() > 0) {
            processGeneAssociation(m, r, geneAssociation);

            if (r.getUserObject(GO_PROCESS_ID_SET) != null) {
              @SuppressWarnings("unchecked")
              Set<String> annoSet = (Set<String>) r.getUserObject(GO_PROCESS_ID_SET);

              // don't put the GO annotation on the reaction, put it on the geneProduct instead
              
//              if (annoSet.size() > 0) {
//                CVTerm cvTerm = new CVTerm();
//                cvTerm.setQualifier(Qualifier.BQB_IS_PART_OF);
//                r.addCVTerm(cvTerm);
//
//                for (String anno : annoSet) {
//                  cvTerm.addResource(IDENTIFIERS_ORG + "go/" + anno);
//                }
//              }
            }
            if (r.getUserObject(EC_CODE_ID_SET) != null) {
              @SuppressWarnings("unchecked")
              Set<String> annoSet = (Set<String>) r.getUserObject(EC_CODE_ID_SET);

              if (annoSet.size() > 0) { 
                CVTerm cvTerm = new CVTerm();
                cvTerm.setQualifier(Qualifier.BQB_IS_VERSION_OF);
                r.addCVTerm(cvTerm);

                for (String anno : annoSet) {
                  cvTerm.addResource(IDENTIFIERS_ORG + "ec-code/" + anno);
                }
              }
            }
          }
          
          // set math and bounds if needed
          if (!r.isSetKineticLaw() || !r.getKineticLaw().isSetMath()) {
            if (m.getLevel() == 2) 
            {
              KineticLaw kl = r.createKineticLaw();
              kl.setMath(ASTNode.parseFormula("FLUX_VALUE"));
              
              LocalParameter lb = new LocalParameter(lowerFluxBound);

              if (!r.isReversible()) {
                lb.setValue(0);
              }
              kl.addLocalParameter(new LocalParameter(lb));
              kl.addLocalParameter(new LocalParameter(upperFluxBound));
              kl.addLocalParameter(fluxValue.clone());
              
            }
            else if (m.getLevel() == 3) 
            {
              FBCReactionPlugin fbcR = (FBCReactionPlugin) r.getPlugin(FBCConstants.shortLabel);
              
              if (r.isReversible()) {
                fbcR.setLowerFluxBound(lowerFluxBound);
              }
              else {
                fbcR.setLowerFluxBound(zeroFluxBound);
              }
              fbcR.setUpperFluxBound(upperFluxBound);
              
              // set the GeneProductAssociation - make use of the new ModelPolisher utility
              if (geneAssociation != null && geneAssociation.trim().length() > 0) {
                org.sbml.jsbml.ext.fbc.converters.GPRParser.parseGPR(r, geneAssociation, false, false);
              }
            }
          }
          
        } else {
          System.out.println("Process reactions - problem found " + tokens.length + " tokens.");
        }
      }
      
      if (m.getLevel() == 3) {
        // annotate the GeneProduct
        annotateGeneProducts(m);
      }
      
      if (unknownPathwaySet.size() > 0) {
        System.out.println("Unknown pathway/group :\n" + unknownPathwaySet);
      }
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }



  /**
   * 
   * 
   * @param r a {@link Reaction}
   * @param subsystem
   * @return
   */
  private static String processSubSystem(Reaction r, String subsystem) {

    subsystem = subsystem.toLowerCase();
    
    if (subsystem.contains(";")) {
      // case where we have several pathways
      String[] tokens = subsystem.split(";");
      int index = 1;
      
      for (String token : tokens) {
        token = token.toLowerCase().trim();
        
        Group group = groupPathwayMap.get(token);
        
        if (group != null) {
          if (r.getLevel() >= 3) {
            // we don't create the group in L2
            group.createMember("GM_" + r.getId() + "_" + index, r);
          }
        } else {
          System.out.println(";;; Could not find a group for '" + token + "'");
          
          unknownPathwaySet.add(token);
        }
        
        index++;
      }
    } 
    else 
    {
      // single pathway for sure
      Group group = groupPathwayMap.get(subsystem);
      
      if (group != null) {
        if (r.getLevel() >= 3) {
          // we don't create the group in L2
          group.createMember("GM_" + r.getId(), r);
        }

      } else {
        System.out.println("sssss Could not find a group for '" + subsystem + "'");
        
        unknownPathwaySet.add(subsystem);
      }
    }
    
    return subsystem;
  }


  /**
   * 
   * @param m the Model
   */
  private static void annotateGeneProducts(Model m) {
    // fbc:id:    G_wormbaseID
    // fbc:label: wormbaseID
    // fbc:name:  Gene symbol if it exists, otherwise locus ID 
    
    FBCModelPlugin fbcM = (FBCModelPlugin) m.getPlugin(FBCConstants.shortLabel);
    
    if (fbcM.getGeneProductCount() > 0) {
      
      for (GeneProduct gp : fbcM.getListOfGeneProducts()) {
        String gpId = gp.getId();
        
        String wormBaseId = gpId.substring(2);
        
        // System.out.println("Wormbase Id = " + wormBaseId);
        gp.setLabel(wormBaseId);
        
        if (wormBaseId.startsWith("WBG")) {
          gp.addCVTerm(new CVTerm(Qualifier.BQB_IS, IDENTIFIERS_ORG + "wormbase/" + wormBaseId));
        }
        
        WGene wGene = geneMap.get(wormBaseId);
        
        if (wGene != null) {
          
          // no annotations to encode apart for the wormbase id
          if (wGene.getSymbol() != null) { // TODO - Put back the name first if we correct the Gene table
            gp.setName(wGene.getSymbol());
          } else if (wGene.getLocus() != null) {
            gp.setName(wGene.getLocus());
          }

          // Go process annotations
          List<String> annoSet = wGene.getGoProcess();
          
          if (annoSet.size() > 0) {
            CVTerm cvTerm = new CVTerm();
            cvTerm.setQualifier(Qualifier.BQB_OCCURS_IN);
            gp.addCVTerm(cvTerm);

            for (String anno : annoSet) {
              cvTerm.addResource(IDENTIFIERS_ORG + "go/" + anno);
            }
          }

          // put locus and other in the notes ? 
          
        } else {
          if (wormBaseId.equals("Unknown") || wormBaseId.equals("TBD") || wormBaseId.equals("ND") || wormBaseId.equals("NA")) {
            // fine. don't report anything
          } else if (!geneIDMissing.contains(wormBaseId)) {
            geneIDMissing.add(wormBaseId);
            System.out.println("WARNING - no entry found for '" + wormBaseId + "'");
          }
        }
      }
      
    } else {
      System.out.println("Warning - No GeneProduct created !!");
    }
  }


  /**
   * 
   * @param r the reaction
   * @param reactionFormula the formula
   */
  private static void parseReactionformula(Reaction r, String reactionFormula) {

    Model m = r.getModel();
    String[] tokens = reactionFormula.split("<=>");
    
    if (tokens.length > 2) {
      System.out.println("Warning - something is wrong with this reaction formula: '" + reactionFormula + "'");
      return;
    }
    
    String[] reactantTokens = tokens[0].split("\\+");
    
    if (tokens[0].trim().length() > 0) { // prevent problem with empty reactant reaction
      for (String reactantStr : reactantTokens) {
        String[] reactantSubtokens = reactantStr.trim().split(" ");
        String speciesId = null;
        Double stoichiometry = null;

        if (reactantSubtokens.length == 2) {
          String stoichioStr = reactantSubtokens[0].trim();

          if (stoichioStr.startsWith("-")) {
            stoichioStr = stoichioStr.substring(stoichioStr.indexOf("-") + 1);
          }
          try {
            stoichiometry = Double.parseDouble(stoichioStr);
          } catch(NumberFormatException e) {
            System.out.println("Warning - Problem reading the stoichiometry: " + e.getMessage());
          }

          speciesId = reactantSubtokens[1].trim();
        } else if (reactantSubtokens.length == 1) {
          stoichiometry = 1.0;
          speciesId = reactantSubtokens[0].trim();
        } else {
          System.out.println("Warning - something is wrong with this reactant string: '" + reactantStr + "'");
        }

        if (m.getSpecies(speciesId) == null) {
          System.out.println("Warning - something is wrong with this reactant species id: '" + speciesId + "'");
          speciesReferenceMissing.add(speciesId);
          // automatically adding a species
          m.createSpecies(speciesId);
        }
        
        if (m.getSpecies(speciesId) != null) {
          SpeciesReference sr = r.createReactant();
          sr.setSpecies(speciesId);
          
          if (m.getLevel() == 3) {
            sr.setConstant(true);
          }

          if (stoichiometry != null) {
            sr.setStoichiometry(stoichiometry);
          }
        }
      }
    }

    if (tokens.length > 1 && tokens[1].trim().length() > 0) { // prevent problem with empty product reaction

      String[] productTokens = tokens[1].split("\\+");
      
      for (String productStr : productTokens) {
        String[] productSubtokens = productStr.trim().split(" ");
        String speciesId = null;
        Double stoichiometry = null;

        if (productSubtokens.length == 2) {
          String stoichioStr = productSubtokens[0].trim();

          if (stoichioStr.indexOf("-") != -1) {
            stoichioStr.substring(stoichioStr.indexOf("-"));
          }
          try {
            stoichiometry = Double.parseDouble(stoichioStr);
          } catch(NumberFormatException e) {
            System.out.println("Warning - Problem reading the stoichiometry: " + e.getMessage());
          }

          speciesId = productSubtokens[1].trim();
        } else if (productSubtokens.length == 1) {
          stoichiometry = 1.0;
          speciesId = productSubtokens[0].trim();
        } else {
          System.out.println("Warning - something is wrong with this product string: '" + productStr + "'");
        }

        if (m.getSpecies(speciesId) == null) {
          System.out.println("Warning - something is wrong with this product species id: '" + speciesId + "'");
          speciesReferenceMissing.add(speciesId);
          // automatically adding a species
          m.createSpecies(speciesId);
        }
        
        if (m.getSpecies(speciesId) != null) {
          SpeciesReference sr = r.createProduct();
          sr.setSpecies(speciesId);

          if (m.getLevel() == 3) {
            sr.setConstant(true);
          }

          if (stoichiometry != null) {
            sr.setStoichiometry(stoichiometry);
          }
        }
      }
    }
  }


  /**
   * 
   * @param geneAssociationStr
   * @param ecNumber
   * @param subSystem
   */
  private static void processGeneAssociation(Model m, Reaction r, String geneAssociationStr) 
  {
      CobraFormulaParser cobraParser = new CobraFormulaParser(new StringReader(""));
      
      try 
      {
        // System.out.println("Gene Association to parse: '" + geneAssociationStr + "'");
        
        ASTNode geneAssociationAST = ASTNode.parseFormula(geneAssociationStr, cobraParser);
        
        //System.out.println("mapGeneAssociation - ASTNode type = " + geneAssociationAST.getType() + ", nb children = " + geneAssociationAST.getChildCount());
        
        if (geneAssociationAST.getType().equals(ASTNode.Type.NAME)) {
          processGeneId(m, r, geneAssociationAST.getName());
        }
        else 
        {
          processGeneAssociation(m, r, geneAssociationAST);
        }
      }
      catch (ParseException e) 
      {
        e.printStackTrace();
      }
  }



  /**
   * @param geneAssociationAST
   * @param ecNumber
   * @param subSystem
   */
  private static void processGeneAssociation(Model m, Reaction r, ASTNode geneAssociationAST) 
  {
    for (ASTNode node : geneAssociationAST.getChildren()) 
    {
      if (node.getType().equals(ASTNode.Type.NAME)) {
        processGeneId(m, r, node.getName());
      }
      else 
      {      
        if (node.getChildCount() > 0) 
        {
          processGeneAssociation(m, r, node);
        }
        else 
        {
          
          System.out.println("WARNING: !!!!!!!!!!!!! ASTNode seems to be wrong !!!!!!!!!!!!!!!!!!");
        }
      }
    }
  }



  /**
   * @param fullId
   * @param ecNumber
   * @param subSystem
   */
  @SuppressWarnings("unchecked")
  private static void processGeneId(Model m, Reaction r, String fullId) 
  {
    String id = fullId;
    
    if (fullId.contains(".")) {
      id = fullId.substring(0, fullId.indexOf("."));
    }
    if (id.startsWith("LOC")) {
      id = id.substring(3);
    }
    
    // System.out.println("mapGeneAssociation - fullId = " + fullId + ", id = " + id + ", found = ");
    if (geneMap.containsKey(fullId)) {
      // System.out.println("found " + fullId);
      WGene gene = geneMap.get(fullId);
      
      // store set of annotations in the user objects of the reaction 
      if (gene.getGoProcess() != null && gene.getGoProcess().size() > 0) {
        Set<String> goProcessIdSet = (Set<String>) r.getUserObject(GO_PROCESS_ID_SET);
        
        if (goProcessIdSet == null) {
          goProcessIdSet = new HashSet<String>();
          r.putUserObject(GO_PROCESS_ID_SET, goProcessIdSet);
        }
        
        for (String goId : gene.getGoProcess()) {
          goProcessIdSet.add(goId);
        }
      }
      if (gene.getEcCode() != null && gene.getEcCode().size() > 0) {
        Set<String> ecIdSet = (Set<String>) r.getUserObject(EC_CODE_ID_SET);
        
        if (ecIdSet == null) {
          ecIdSet = new HashSet<String>();
          r.putUserObject(EC_CODE_ID_SET, ecIdSet);
        }
        
        for (String ecId : gene.getEcCode()) {
          ecIdSet.add(ecId);
        }
      }
    }
  }

 
  /**
   * 
   * @param file the pathways file
   * @param m the {@link Model}
   */
  private static void processPathways(File file, Model m) {
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(file);

      // File content:
      // pathway_name    go      kegg.pathway_id biocic  pw
      // 
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      GroupsModelPlugin gm = null;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        // System.out.println (strLine);
        
        // removing the header line
        if (strLine.startsWith("#") || strLine.startsWith("!")  || strLine.startsWith("pathway_name") ) {
          continue;
        }
        if (strLine.trim().length() == 0) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        
        if (tokens.length >= 1) {
          
          if (gm == null && m.getLevel() >= 3) {
            gm = (GroupsModelPlugin) m.getPlugin(GroupsConstants.shortLabel);
          } else if (gm == null && m.getLevel() == 2) {
            gm = new GroupsModelPlugin(m);
          }
          
          Group g = null;
          
          if (strLine.contains("[DELETION]")) {
            // The group is deleted so we do not add it to the model
            g = new Group(3, 1); 
          } else {
            g = gm.createGroup();
          }
          g.setKind(Group.Kind.partonomy);

          String groupPathwayName = tokens[0].trim();;

          g.setName(groupPathwayName);
          g.setId(nameToId(g.getName()));
          
          groupPathwayMap.put(groupPathwayName.toLowerCase(), g);

          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            // GO annotation
            String[] goAnnos = tokens[2].trim().split(";");
            CVTerm goCVTerm = new CVTerm(CVTerm.Qualifier.BQB_IS);
            
            g.addCVTerm(goCVTerm);

            for (String goAnno :  goAnnos) {
              goAnno = goAnno.trim();
              
              goCVTerm.addResource(IDENTIFIERS_ORG + "go/" + goAnno);
            }
          }
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            // kegg.pathway annotation
            String keggAnno = tokens[3].trim();
            
            if (keggAnno.length() > 0 && !keggAnno.contains("?")) {
              CVTerm pwCVTerm = new CVTerm(CVTerm.Qualifier.BQB_IS, IDENTIFIERS_ORG + "kegg.pathway/" + keggAnno);
              
              g.addCVTerm(pwCVTerm);              
            }
          }
          if (tokens.length > 4 && tokens[4].trim().length() > 0) {
            // biocyc annotation
            String bioCycAnno = tokens[4].trim();
            
            if (bioCycAnno.length() > 0 && !bioCycAnno.contains("?")) {
              CVTerm bioCycCVTerm = new CVTerm(CVTerm.Qualifier.BQB_IS, IDENTIFIERS_ORG + "biocyc/" + bioCycAnno);
              
              g.addCVTerm(bioCycCVTerm);              
            }
          }
          if (tokens.length > 5 && tokens[5].trim().length() > 0) {
            // pw annotation
            String[] goAnnos = tokens[5].trim().split(";");
            CVTerm pwCVTerm = new CVTerm(CVTerm.Qualifier.BQB_IS);
            
            g.addCVTerm(pwCVTerm);

            for (String pwAnno :  goAnnos) {
              pwAnno = pwAnno.trim();
              
              pwCVTerm.addResource(IDENTIFIERS_ORG + "pw/" + pwAnno);
            }
          }
          
        } else {
          System.out.println("Process pathways - problem found " + tokens.length + " tokens.");
        }
      }
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }
  }


  /**
   * @param name
   * @return
   */
  private static String nameToId(String name) {
    String id = "P_" + name.replaceAll(" ", "_");
    id = id.replaceAll(",", "_");
    id = id.replaceAll("-", "_");
    id = id.replaceAll("\\(", "_");
    id = id.replaceAll("\\)", "_");
    id = id.replaceAll("'", "_");
    id = id.replaceAll("/", "_");
    
    return id;
  }

}
