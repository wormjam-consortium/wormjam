package org.sbml.wormjam;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.validator.SyntaxChecker;

/**
 * 
 * 
 * @author rodrigue
 *
 */
public class RemoveComplexChemicalFormula {

  /**
   * 
   */
  private static Set<String> removedFormulae = new HashSet<String>();

  /**
   * Compare two SBML files passes as arguments.
   * 
   * @param args program arguments
   */
  public static void main(String[] args) 
  {
    if (args.length < 1) {
      System.out.println("We expect to get one argument that point to an SBML file to correct");
      System.exit(0);
    }
    
    try {
      SBMLDocument doc = new SBMLReader().readSBMLFromFile(args[0]);

      for (Species s : doc.getModel().getListOfSpecies()) {
        
        String formula = null;
        
        if (s.isSetPlugin("fbc")) {
          FBCSpeciesPlugin fbcS = (FBCSpeciesPlugin) s.getPlugin("fbc");
          
          if (fbcS.isSetChemicalFormula()) {
            formula = fbcS.getChemicalFormula();
            
            if (formula != null && formula.contains("(")) {
              fbcS.unsetChemicalFormula();
              removedFormulae.add(formula);
            }
            if (formula != null && !SyntaxChecker.isValidChemicalFormula(formula)) {
              fbcS.unsetChemicalFormula();
              removedFormulae.add(formula);              
            }
            
          }
        }
      }

      for (String rf : removedFormulae) {
        System.out.println("\t'" + rf + "'");
      }
      
      String fileName = args[0];
      String jsbmlWriteFileName = fileName.replaceFirst("\\.xml", "-removedFormula.xml");
      
      new SBMLWriter().writeSBMLToFile(doc, jsbmlWriteFileName);
      
    } catch (XMLStreamException | IOException e) {
      e.printStackTrace();
    }

  }
}
