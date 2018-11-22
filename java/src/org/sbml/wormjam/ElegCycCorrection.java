package org.sbml.wormjam;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.TidySBMLWriter;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.text.parser.ParseException;
import org.sbml.jsbml.util.CobraUtil;
import org.sbml.libsbml.ConversionProperties;

/**
 * @author rodrigue
 *
 */
public class ElegCycCorrection {

  /**
   * 
   */
  private static Map<String, String> wormBaseGeneAndSequenceToID = new HashMap<String, String>();
  
  /**
   * 
   */
  private static Set<String> wormBaseMultiMapping = new HashSet<String>();
  
  /**
   * 
   * @param args the arguments, expecting one file path
   * @throws XMLStreamException if error occurs
   * @throws IOException if error occurs
   * @throws ParseException  if error occurs
   */
  @SuppressWarnings("deprecation")
  public static void main(String[] args) throws XMLStreamException, IOException, ParseException {
    
    SBMLDocument doc = new SBMLReader().readSBMLFromFile(args[0]);

    String[] reactsIds = new String[6];
    String[] reactsIds150 = new String[150];
    String[] reactsIds666 = new String[666];
    String[] reactsIds1005 = new String[1005];
    int i = 0;
    
    for (Reaction reaction : doc.getModel().getListOfReactions()) {
      if (reaction.isSetKineticLaw() && (! reaction.getKineticLaw().isSetMath()))
      {
        KineticLaw kl = reaction.getKineticLaw();
        kl.setMath(ASTNode.parseFormula("FLUX_VALUE"));
        
        LocalParameter fluxValue = new LocalParameter("FLUX_VALUE");
        // fluxValue.setUnits("mmol_per_gDW_per_hr");
        fluxValue.setValue(0);
        
        kl.addLocalParameter(fluxValue);
      }
      else if (reaction.isSetKineticLaw() && reaction.getKineticLaw().isSetMath()) {
        // System.out.println("WARNING: math defined for reaction '" + reaction.getId() + "'");
        LocalParameter fluxValue = reaction.getKineticLaw().getLocalParameter("FLUX_VALUE");
        
        if (fluxValue != null) {
          fluxValue.unsetUnits();
        }
      }
      
      if (i < 6) {
        reactsIds[i] = reaction.getId();        
      }
      if (i < 150) {
        reactsIds150[i] = reaction.getId();
      }
      if (i < 666) {
        reactsIds666[i] = reaction.getId();
      }
      if (i < 1005) {
        reactsIds1005[i] = reaction.getId();
      }

      // TODO - check the gene association string and delete it if it contain 'NA' or 'unknow'
      
      i++;
    }
    
    int nbChebi = 0;
    int nbAnnotated = 0;
    int nbFormula = 0;
    int nbFormulaButNoChebi = 0;
    Set<String> notesKey = new HashSet<String>();
    
    // Possible annotation in the notes: CHEBI, PUBCHEM, LIGANDCPD, CAS, KNAPSACKs
    // real list from elegCyc: UNIPROT, PFAM, InChi, Wikipedia, PUBCHEM, FORMULA, UMBBDCPD, ECOCYC, CAS, SWISSMODEL, NCI, CHEBI, MODBASE, KNAPSACKs, LIGANDCPD, CHARGE, KNAPSACK
    notesKey.add("CHEBI");
    notesKey.add("PUBCHEM");
    notesKey.add("LIGANDCPD");
    notesKey.add("CAS");
    notesKey.add("KNAPSACKs");
    notesKey.add("FORMULA");
    notesKey.add("CHARGE");
    
    // for iCEL1273, the id of the species is the Kegg id, the 'C' is replaced by 'M' and 'E' for the different compartments. Sometimes a '_x' is added at the end if the same kegg id is used for different sbml species. 
    
    for (Species species : doc.getModel().getListOfSpecies()) {
      boolean hasChebi = false;
      
      if (species.filterCVTerms(null, ".*chebi.*").size() > 0) {
        nbChebi++;
        hasChebi = true;
      }
      
      // check formula
      if (species.isSetNotes()) {
        Properties p = CobraUtil.parseCobraNotes(species); // TODO - add 'WithP' in the method name or correct JSBML
        
        // System.out.println("DEBUG - cobra properties = " + p);
        
        if (p.getProperty("CHEBI") != null) {
          hasChebi = true;
          nbChebi++;
        }
        
        if (hasChebi || p.getProperty("PUBCHEM") != null || p.getProperty("LIGANDCPD") != null || p.getProperty("CAS") != null
            || p.getProperty("KNAPSACKs") != null || p.getProperty("KNAPSACK") != null || p.getProperty("UNIPROT") != null) 
        {
          nbAnnotated++;
        }
        
        String formula = p.getProperty("FORMULA");
        
        if (formula != null && !formula.equals("R")) {
          nbFormula++;
          
          if (!hasChebi) {
            nbFormulaButNoChebi++;
          }
        }
        
        for (Object key : p.keySet()) {
          notesKey.add((String) key);
        }
      }
    }
    
    System.out.println("DEBUG - cobra properties keys = " + notesKey);
    
    System.out.println("There are '" + doc.getModel().getNumSpecies() + "' species in the model. " + nbChebi + " have a chebi annotation, "
        + nbAnnotated + " have an annotation, " + nbFormula + " have a chemical formula and " + nbFormulaButNoChebi + " have a formula but no chebi annotation.");
    
    String fileName = new File(args[0]).getAbsolutePath();
    String jsbmlWriteFileName = fileName.replaceFirst("\\.xml", "-jsbml-L2V1.xml");

    //
    // Saving the full corrected L2V1 model
    //
    new TidySBMLWriter().writeSBMLToFile(doc, jsbmlWriteFileName);
    

        
    /*
    //
    // making smaller submodels for testing purposes
    //
    
    SBMLDocument smallModel = SubModel.generateSubModel(doc.getModel(), null, null, reactsIds);
    smallModel.getModel().getCompartment(0).unsetOutside();
    
    System.out.println("They are '" + smallModel.getModel().getNumSpecies() + "' species in the submodel");
    
    jsbmlWriteFileName = fileName.replaceFirst("\\.xml", "-jsbml-L2V1-small.xml");

    new TidySBMLWriter().writeSBMLToFile(smallModel, jsbmlWriteFileName);
    
    // 150 reactions model (363 species)
    smallModel = SubModel.generateSubModel(doc.getModel(), null, null, reactsIds150);
    smallModel.getModel().getCompartment(0).unsetOutside();
    
    System.out.println("They are '" + smallModel.getModel().getNumSpecies() + "' species in the submodel with 150 reactions");
    
    new TidySBMLWriter().writeSBMLToFile(smallModel, fileName.replaceFirst("\\.xml", "-jsbml-L2V1-small-150.xml"));
    
    
    // 150 reactions, all species model
    Model sm = smallModel.getModel();
    for (Species s : doc.getModel().getListOfSpecies())
    {
      if (sm.getSpecies(s.getId()) == null) {
        sm.addSpecies(s.clone());
      }
    }
    for (Compartment c : doc.getModel().getListOfCompartments())
    {
      if (sm.getCompartment(c.getId()) == null) {
        sm.addCompartment(c.clone());
      }
    }
    
    System.out.println("They are '" + smallModel.getModel().getNumSpecies() + "' species in the submodel with 150 reactions, all species");    
    new TidySBMLWriter().writeSBMLToFile(smallModel, fileName.replaceFirst("\\.xml", "-jsbml-L2V1-small-150-allspecies.xml"));

    
    // 666 reactions model (1035 species) 
    smallModel = SubModel.generateSubModel(doc.getModel(), null, null, reactsIds666);
    smallModel.getModel().getCompartment(0).unsetOutside();
    
    System.out.println("They are '" + smallModel.getModel().getNumSpecies() + "' species in the submodel with 666 reactions");
    
    new TidySBMLWriter().writeSBMLToFile(smallModel, fileName.replaceFirst("\\.xml", "-jsbml-L2V1-small-666.xml"));

    // 1005 reactions model
    smallModel = SubModel.generateSubModel(doc.getModel(), null, null, reactsIds1005);
    smallModel.getModel().getCompartment(0).unsetOutside();
    
    System.out.println("They are '" + smallModel.getModel().getNumSpecies() + "' species in the submodel with 1005 reactions");
    
    new TidySBMLWriter().writeSBMLToFile(smallModel, fileName.replaceFirst("\\.xml", "-jsbml-L2V1-small-1005.xml"));
    */
    
    

    //
    //
    // Doing the conversion using libsbml converter from COBRA to FBC
    //
    // 
    org.sbml.libsbml.SBMLDocument ldoc = new org.sbml.libsbml.SBMLReader().readSBMLFromString(new TidySBMLWriter().writeSBMLToString(doc));
    
    System.out.println("libsbml version = " + org.sbml.libsbml.libsbml.getLibSBMLDottedVersion());
    
    /* create a new conversion properties structure */
    ConversionProperties props = new ConversionProperties();
                
    /* add an option that we want to convert a model  with
       L2 with COBRA annotation to L3 FBC v1 */
    props.addOption("convert cobra", true, "Convert Cobra model to FBC model");
    
    int libsbmlReturnCode = ldoc.convert(props);
    
    /* perform the conversion */
    if (libsbmlReturnCode != org.sbml.libsbml.libsbmlConstants.LIBSBML_OPERATION_SUCCESS)
    {
      System.out.println("conversion to FBC v1 failed ... ");
      System.exit(3); 
    }
    // new org.sbml.libsbml.SBMLWriter().writeSBMLToString(ldoc); // , fileName.replaceFirst("\\.xml", "-libsbml-L3V1-FBCv1.xml")
  
    /* create a new conversion properties structure */
    props = new ConversionProperties();
                
    /* add an option that we want to convert a model  with
       L3 FBC v1 to FBC v2 */
    props.addOption("convert fbc v1 to fbc v2", true, "convert fbc v1 to fbc v2");
    props.addOption("strict", true, "should the model be a strict one (i.e.: all non-specified bounds will be filled)");
    
    libsbmlReturnCode = ldoc.convert(props);
    
    /* perform the conversion */
    if (libsbmlReturnCode != org.sbml.libsbml.libsbmlConstants.LIBSBML_OPERATION_SUCCESS)
    {
      System.out.println("conversion to FBC v2 failed ... ");
      System.exit(3); 
    }
    String sbmlDocStringFbcv2 = new org.sbml.libsbml.SBMLWriter().writeSBMLToString(ldoc); // , fileName.replaceFirst("\\.xml", "-libsbml-L3V1-FBCv2.xml")
    
    //
    // Reading the converted file back with jsbml to improve annotations and indentation of the xml
    //
    SBMLDocument docV2 = new SBMLReader().readSBMLFromString(sbmlDocStringFbcv2);
    
    // making a map of wormbase gene names and sequence ids    
    createWormBaseMaps();
    
    // going through all geneProduct and trying to annotate them with a wormBase id.
    if (docV2.getModel().isSetPlugin("fbc")) {
      FBCModelPlugin fbcModel = (FBCModelPlugin) docV2.getModel().getPlugin("fbc");
      
      for (GeneProduct gp : fbcModel.getListOfGeneProducts()) {
        String gpLabel = gp.getLabel();
  
        if (gpLabel.startsWith("CELE_")) {
          gpLabel = gpLabel.substring(5);
        }
  
        if (wormBaseGeneAndSequenceToID.get(gpLabel) != null && (!wormBaseMultiMapping.contains(gpLabel))) {
          gp.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS, "http://identifiers.org/wormbase/" + wormBaseGeneAndSequenceToID.get(gpLabel)));
        }
        else {
          System.out.println("no mapping found for '" + gp.getLabel() + "'");
        }
      }
    }
    
    new SBMLWriter().writeSBMLToFile(docV2, fileName.replaceFirst("\\.xml", "-jsbml-L3V1-FBCv2.xml")); // , 
    
    //
    // saving a L3V1 version as well, converted by hand
    //
    simpleConvertToL3(doc);
    
    System.out.println("They are '" + doc.getModel().getNumSpecies() + "' species in the main model with '" + doc.getModel().getNumReactions() + "' reactions");
    
    jsbmlWriteFileName = fileName.replaceFirst("\\.xml", "-jsbml-L3V1-noFBC.xml");

    new TidySBMLWriter().writeSBMLToFile(doc, jsbmlWriteFileName);
  }



  /**
   * 
   * @param doc
   * @throws ParseException
   */
  private static void simpleConvertToL3(SBMLDocument doc)
      throws ParseException {
    doc.setLevelAndVersion(3, 1);
    
    for (Compartment comp : doc.getModel().getListOfCompartments()) {
      if (! comp.isSetConstant()) {
        comp.setConstant(true);
      }
    }
    
    for (Species species : doc.getModel().getListOfSpecies()) {
      if (! species.isSetHasOnlySubstanceUnits()) {
        species.setHasOnlySubstanceUnits(false);
      }
      if (! species.isSetBoundaryCondition()) {
        species.setBoundaryCondition(false);
      }
      if (! species.isSetConstant()) {
        species.setConstant(false);
      }
      if (species.isSetUnits()) {
        System.out.println("Species units set to '" + species.getUnits() + "'");
      }
    }
    
    for (Reaction reaction : doc.getModel().getListOfReactions()) {
      if (reaction.isSetKineticLaw() && (! reaction.getKineticLaw().isSetMath()))
      {
        KineticLaw kl = reaction.getKineticLaw();
        kl.setMath(ASTNode.parseFormula("FLUX_VALUE"));
        
        LocalParameter fluxValue = new LocalParameter("FLUX_VALUE");
        // fluxValue.setUnits("mmol_per_gDW_per_hr");
        fluxValue.setValue(0);
        
        kl.addLocalParameter(fluxValue);
        
      }
      
      if (! reaction.isSetFast()) {
        reaction.setFast(false); // default in L2V1
      }

      if (! reaction.isSetReversible()) {
        reaction.setReversible(true); // default in L2V1
      }

      for (SpeciesReference speciesRef : reaction.getListOfProducts()) {
        if (! speciesRef.isSetConstant()) {
          speciesRef.setConstant(true); // default in L2V1
        }
      }
      for (SpeciesReference speciesRef : reaction.getListOfReactants()) {
        if (! speciesRef.isSetConstant()) {
          speciesRef.setConstant(true); // default in L2V1
        }
      }      
    }
  }
  
  
  
  /**
   * 
   */
  private static void createWormBaseMaps() {

    int nbLines = 0;

    try{
      // Open the file
      FileInputStream fstream = new FileInputStream("/bi/group/compneur/worm/wormbase/c_elegans.PRJNA13758.WS259.geneIDs.txt.gz");

      // File content:
      // number,WormBase Id, gene name, sequence name, Live
      // 6239,WBGene00000024,abu-1,AC3.3,Live
      // 6239,WBGene00000011,abc-1,,Live
      // 6239,WBGene00000052,,,Dead
      //
      
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(new GZIPInputStream(fstream));
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        // System.out.println (strLine);
        
        // removing the header line
        if (strLine.startsWith("#") || strLine.startsWith("HGNC ID")) {
          continue;
        }
        
        nbLines++;
        
        String[] tokens = strLine.split(",");
        
        if (tokens.length >= 5) {
          String wormbaseId = tokens[1];
          String geneName = tokens[2];
          String sequenceId = tokens[3];

          // System.out.println("Wormbase_ID = '" + wormbaseId + "', gene = '" + geneName + "', sequence = '" + sequenceId + "'");
          
          if (sequenceId.trim().length() > 0) {
            if (wormBaseGeneAndSequenceToID.get(sequenceId) != null) {
              System.out.println("!!!!!!!!!!!!!!!! WARNING wormBase mapping not unique !! for = " + wormBaseGeneAndSequenceToID.get(sequenceId) + ", " + wormbaseId + ", " + sequenceId);
              wormBaseMultiMapping.add(sequenceId);
            }
            
            wormBaseGeneAndSequenceToID.put(sequenceId, wormbaseId);
          }
    
          if (geneName.trim().length() > 0) {
            if (wormBaseGeneAndSequenceToID.get(geneName) != null) {
              System.out.println("!!!!!!!!!!!!!!!! WARNING wormBase mapping not unique !! for = " + wormBaseGeneAndSequenceToID.get(geneName) + ", " + wormbaseId + ", " + geneName);
              wormBaseMultiMapping.add(geneName);
            }
            
            wormBaseGeneAndSequenceToID.put(geneName, wormbaseId);
          }
          
        } else {
          System.out.println("Found " + tokens.length + " tokens: " + Arrays.toString(tokens));
        }
      }
        //Close the input stream
        in.close();
      } catch (Exception e){//Catch exception if any
        System.err.println("Error: " + e.getMessage());
        e.printStackTrace();
      }
    
    System.out.println("WormBase Gene IDS analyzed: " + nbLines + " entries found. " + wormBaseGeneAndSequenceToID.size() + " mappings found.");
  }



  /**
   * Loads the SWIG-generated libSBML Java module when this class is
   * loaded, or reports a sensible diagnostic message about why it failed.
   */
  static
  {
    try
    {
      System.loadLibrary("sbmlj");
      // For extra safety, check that the jar file is in the classpath.
      Class.forName("org.sbml.libsbml.libsbml");
    }
    catch (UnsatisfiedLinkError e)
    {
      System.err.println("Error encountered while attempting to load libSBML:");
      System.err.println("Please check the value of your "
                         + (System.getProperty("os.name").startsWith("Mac OS")
                            ? "DYLD_LIBRARY_PATH" : "LD_LIBRARY_PATH") +
                         " environment variable and/or your" +
                         " 'java.library.path' system property (depending on" +
                         " which one you are using) to make sure it list the" +
                         " directories needed to find the " +
                         System.mapLibraryName("sbmlj") + " library file and" +
                         " libraries it depends upon (e.g., the XML parser).");
      System.exit(1);
    }
    catch (ClassNotFoundException e)
    {
      System.err.println("Error: unable to load the file 'libsbmlj.jar'." +
                         " It is likely that your -classpath command line " +
                         " setting or your CLASSPATH environment variable " +
                         " do not include the file 'libsbmlj.jar'.");
      e.printStackTrace();

      System.exit(1);
    }
    catch (SecurityException e)
    {
      System.err.println("Error encountered while attempting to load libSBML:");
      e.printStackTrace();
      System.err.println("Could not load the libSBML library files due to a"+
                         " security exception.\n");
      System.exit(1);
    }
  }
}
