package org.sbml.wormjam;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.util.SubModel;

/**
 * 
 * 
 * @author rodrigue
 *
 */
public class CompareWormJamWithCeCon {

  /**
   * 
   */
  private static HashMap<String, ArrayList<String>> reactionFormulas = new HashMap<String, ArrayList<String>>();  
  
  /**
   * 
   */
  private static Set<String> missingReactionIds = new HashSet<String>();

  /**
   * 
   */
  private static HashMap<String, List<String>> matchedDuplicateForMissingReactions = new HashMap<String, List<String>>();  

  /**
   * 
   */
  private static Set<String> modifiedReactionIds = new HashSet<String>();

  /**
   * 
   */
  private static Set<String> reversibilityChangedReactionIds = new HashSet<String>();

  /**
   * Compare two SBML files passes as arguments.
   * 
   * @param args program arguments
   */
  public static void main(String[] args) 
  {
    if (args.length < 2) {
      System.out.println("We expect to get at least two arguments that point to two SBML files to compare");
      System.exit(0);
    }
    
    try {
      SBMLDocument wormJamDoc = new SBMLReader().readSBMLFromFile(args[0]);
      SBMLDocument ceConDoc = new SBMLReader().readSBMLFromFile(args[1]);

      // TODO - try to make a mapping between species in both model 
      for (Species s1 : wormJamDoc.getModel().getListOfSpecies()) {
        
        String id = s1.getId();
        
        if (ceConDoc.getModel().getSBaseById(id) == null) {
          System.out.println("Missing species with id '" + id + "'.");
        }
      }

      // Init the reaction formulas
      for (Reaction r1 : wormJamDoc.getModel().getListOfReactions()) {
        String formula = getReactionFormula(r1);
        String reverseFormula = getReverseReactionFormula(r1);
        String reactionId = r1.getId().replaceAll("R_R_", "R_");
        ArrayList<String> reactionIds = null; 
        
        if (reactionFormulas.get(formula) != null) {
          reactionIds = reactionFormulas.get(formula);
        } else {
          reactionIds = new ArrayList<String>();
          reactionFormulas.put(formula, reactionIds);
          reactionFormulas.put(reverseFormula, reactionIds);
        }
        
        if (! reactionIds.contains(reactionId)) {
          reactionIds.add(reactionId);
        }
        
        if (reactionFormulas.get(formula) != null) { // TODO - should it be 'reverseFormula' here ? The code seems duplicated otherwise
          reactionIds = reactionFormulas.get(formula);
        } else {
          reactionIds = new ArrayList<String>();
          reactionFormulas.put(formula, reactionIds);
          reactionFormulas.put(reverseFormula, reactionIds);
        }
        
        if (! reactionIds.contains(reactionId)) {
          reactionIds.add(reactionId);
        }
      }

      System.out.println("Duplicated reactions in WormJam:");
      for (Reaction r1 : wormJamDoc.getModel().getListOfReactions()) {
        String formula = getReactionFormula(r1);
        
        if (reactionFormulas.get(formula) != null && reactionFormulas.get(formula).size() > 1) {
          if (reactionFormulas.get(formula).size() > 1) {
            System.out.println("Formula = '" + formula + "', reaction ids = " + reactionFormulas.get(formula));
          }
        }
      }
      
      // TODO - compare reactions and try to find the reactions from CeCon that are not present in WormJam
      // TODO - convert gene association string to compare it as well ?
      int missingReactions = 0;
      for (Reaction r2 : ceConDoc.getModel().getListOfReactions()) {
        
        String id = r2.getId();
        
        if (wormJamDoc.getModel().getSBaseById(id) == null && wormJamDoc.getModel().getSBaseById("R_" + id) == null) {
          // System.out.println("Not the same reaction id for '" + id + "'.");
          missingReactions++;
          missingReactionIds.add(id);
          
          String formula = getReactionFormula(r2);
          
          if (reactionFormulas.containsKey(formula)) {
            matchedDuplicateForMissingReactions.put(id, reactionFormulas.get(formula));
          }
        }
        else {
          Reaction r1 = (Reaction) wormJamDoc.getModel().getSBaseById("R_" + id);
          
          if (r1 == null) {
            r1 = (Reaction) wormJamDoc.getModel().getSBaseById(id);
          }
          
          // checkReversibility(r1, r2);
          
          String formula1 = getReactionFormula(r1);
          String formula2 = getReactionFormula(r2);
          
          if (! formula1.equals(formula2)) {
            formula1 = formula1.replace("<", "");
            formula2 = formula2.replace("<", "");

            if (! formula1.equals(formula2)) {
              modifiedReactionIds.add(id);
            
              System.out.println("Modified reaction '" + id + "': \n1: " + formula1 + "\n2: " + formula2);
            } else {
              reversibilityChangedReactionIds.add(id);
              // System.out.println("Modified reaction reversibility for '" + id + "'");
            }
          }
        }
      }
      
      
      
      System.out.println("Doc1 has " + wormJamDoc.getModel().getNumSpecies() + " species and " + wormJamDoc.getModel().getNumReactions() + " reactions.");
      System.out.println("Doc2 has " + ceConDoc.getModel().getNumSpecies() + " species and " + ceConDoc.getModel().getNumReactions() + " reactions.");
      System.out.println("We found " + missingReactions + " missing reactions. (" + missingReactionIds.size() + ")");
      System.out.println("We found " + reversibilityChangedReactionIds.size() + " modified reaction reversibility.\n" + reversibilityChangedReactionIds);
      System.out.println("We found " + modifiedReactionIds.size() + " modified reactions.");
      
      
      SBMLDocument mrdoc = SubModel.generateSubModel(ceConDoc.getModel(), null, null, missingReactionIds.toArray(new String[missingReactionIds.size()]));
      
      new SBMLWriter().writeSBMLToFile(mrdoc, "/home/rodrigue/data/wormReconstructions/2017-11-08/Celecon_v1_missing_reactions.xml");
      
      PrintWriter out = new PrintWriter("/home/rodrigue/data/wormReconstructions/2017-11-08/Celecon_v1_missing_reactions_with_duplicate.csv");

      int nbFoundDuplicate = 0;
      
      out.println("missing_reaction_id\tlist_of_duplicates\treversible_changed\treversible_from_bounds");

      System.out.println("We found " + missingReactions + " missing reactions. (" + missingReactionIds.size() + ")");
      int nbLines = 0;
      
      for (String id : missingReactionIds) {
        String duplicate = "";
        boolean reversibleChanged = false;
        Reaction reaction = ceConDoc.getModel().getReaction(id);
            
        if (matchedDuplicateForMissingReactions.get(id) != null) {
          nbFoundDuplicate++;
          List<String> duplicates = matchedDuplicateForMissingReactions.get(id);
          duplicate = duplicates.toString();
          String formula = getReactionFormula(ceConDoc.getModel().getReaction(id), true);
          
          for (String duplicateId : duplicates) {
            Reaction duplicateReaction = wormJamDoc.getModel().getReaction("R_" + duplicateId);
            
            if (duplicateReaction == null) {
              duplicateReaction = wormJamDoc.getModel().getReaction(duplicateId);
            }
            
            String formulaDuplicate = getReactionFormula(duplicateReaction, true);
            
            if (! formula.equals(formulaDuplicate)) {
              if (duplicateReaction.isReversible() && reaction.isReversible()) {
                // nothing to do
              } else if (duplicateReaction.isReversible()) {
                // We can ignore this case as well as the conserved reaction is already reversible
              } else {
                reversibleChanged = true;
              }
            }
          }
          
        } else {
          // out.println(id + "\t" + duplicate);
        }
        
        out.println(id + "\t" + duplicate + "\t" + reversibleChanged + "\t" + getReversibleFromBounds(reaction));
        nbLines++;
                
      }
      
      System.out.println("Out of the " + missingReactions + " missing reactions, we found " + nbFoundDuplicate + " duplicate.");
      System.out.println("We tried to print " + nbLines + " lines.");
      
      out.flush();
      out.close();
    } catch (XMLStreamException | IOException e) {
      e.printStackTrace();
    }

  }

  /**
   * Checks the value of the reversible flag for 3 given reactions
   * 
   * @param r1 the first reaction
   * @param r2 the second reaction
   * @param r3 the third reaction
   */
  private static void checkReversibility(Reaction r1, Reaction r2, Reaction r3) {
    boolean reversible1 = r1.isReversible();
    boolean reversible2 = r2.isReversible();
    Boolean reversible3 = r3 != null ? r3.isReversible() : null;
    Boolean reversibleB1 = getReversibleFromBounds(r1);
    Boolean reversibleB2 = getReversibleFromBounds(r2);
    Boolean reversibleB3 = getReversibleFromBounds(r3);
    
    if (reversible1 == reversible2 && (reversible3 == null || reversible3 == reversible2)) {
      return;
    }
    if (reversible1 == reversibleB2 && reversible1 == reversibleB1 && (reversibleB3 == null || reversible1 == reversibleB3) && (reversible3 == null || reversible3 == reversible1)) {
      return;
    }
    System.out.println("Modified reversibility '" + r2.getId() + "': \n1: " + reversible1 + "  " + reversibleB1 
      + "\n2: " + reversible2 + "  " + reversibleB2 + "\n3: " + reversible3 + "  " + reversibleB3);
    
    // TODO - add the value from the bounds if different
    
  }

  
  
  /**
   * Returns the value of the reversibility of a reaction determined by the value of the bounds
   * 
   * @param reaction the reaction to check
   * @return the value of the reversibility of a reaction determined by the value of the bounds
   */
  private static Boolean getReversibleFromBounds(Reaction reaction) {
    
    if (reaction == null || !reaction.isSetKineticLaw() || reaction.getKineticLaw().getLocalParameter("LOWER_BOUND") == null) {
      return null;
    }
    
    double lowerBoundValue = reaction.getKineticLaw().getLocalParameter("LOWER_BOUND").getValue();
    boolean reversible = false;
    
    if (lowerBoundValue < 0) {
      if (reaction.isSetReversible() && !reaction.isReversible()) {
        // System.out.println("changing reversibility of reaction '" + reaction.getId() + "' to true");
      }
      reversible = true;;
    } 
    else if (lowerBoundValue == 0) {
      if (reaction.isSetReversible() && reaction.isReversible()) {
        // System.out.println("changing reversibility of reaction '" + reaction.getId() + "' to false");            
      }
      //reaction.setReversible(false);
    }
    
    return reversible;
  }

  /**
   * Creates a String that represent the reactants and products of the reaction without their stoichiometry.
   * 
   * @param r the reaction
   * @return a String that represent the reactants and products of the reaction without their stoichiometry.
   */
  private static String getReactionFormula(Reaction r) {
    return getReactionFormula(r, false);
  }
  
  /**
   * Creates a String that represent the reactants and products of the reaction without their stoichiometry.
   * 
   * @param r the reaction
   * @param addReversiblity a boolean to tell if we should put arrows in the reaction formula
   * @return a String that represent the reactants and products of the reaction without their stoichiometry.
   */
  private static String getReactionFormula(Reaction r, boolean addReversiblity) {
    SortedSet<String> reactantSet = new TreeSet<String>();
    SortedSet<String> productSet = new TreeSet<String>();
    
    for (SpeciesReference sr : r.getListOfReactants()) {
      reactantSet.add(sr.getSpecies());
    }
    
    for (SpeciesReference sr : r.getListOfProducts()) {
      productSet.add(sr.getSpecies());
    }
    
    String formula = reactantSet.toString();
    
    if (!addReversiblity) {
      formula += " -- ";
    } else if (r.isReversible()) {
      formula += " <--> ";
    } else {
      formula += " --> ";
    }
    
    formula += productSet.toString();
    
    return formula;
  }

  /**
   * Creates a String that represent the reactants and products of the reaction without their stoichiometry.
   * 
   * @param r the reaction
   * @return a String that represent the reactants and products of the reaction without their stoichiometry.
   */
  private static String getReverseReactionFormula(Reaction r) {
    SortedSet<String> reactantSet = new TreeSet<String>();
    SortedSet<String> productSet = new TreeSet<String>();
    
    for (SpeciesReference sr : r.getListOfReactants()) {
      reactantSet.add(sr.getSpecies());
    }
    
    for (SpeciesReference sr : r.getListOfProducts()) {
      productSet.add(sr.getSpecies());
    }
    
    String formula = productSet.toString();
    
    formula += " -- ";
    
    formula += reactantSet.toString();
    
    return formula;
  }

}
