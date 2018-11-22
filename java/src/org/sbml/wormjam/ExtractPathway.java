package org.sbml.wormjam;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.xml.stream.XMLStreamException;

import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.util.CobraUtil;

/**
 * 
 * 
 * @author rodrigue
 *
 */
public class ExtractPathway {

  
  /**
   * 
   */
  private static Set<String> pathways = new HashSet<String>();

  /**
   * 
   */
  private static HashMap<String, String> keggPathways = new HashMap<String, String>();
  
  
  /**
   * Compare two SBML files passes as arguments.
   * 
   * @param args program arguments
   */
  public static void main(String[] args) 
  {
    if (args.length < 2) {
      System.out.println("We expect to get at least two arguments that point to an SBML file and a text file with Kegg pathways entries.");
      System.exit(0);
    }
    
    try {
      SBMLDocument doc1 = new SBMLReader().readSBMLFromFile(args[0]);

//      for (Species s : doc1.getModel().getListOfSpecies()) {
//        
//        // something to do for species ??
//      }

      // loop over the reactions
      for (Reaction r : doc1.getModel().getListOfReactions()) {

        // extracting pathway information        
        Properties cobraNotes = CobraUtil.parseCobraNotes(r);
        
        if (cobraNotes.get("SUBSYSTEM") != null) {
          String subSystem = (String) cobraNotes.get("SUBSYSTEM");
          
          if (subSystem.trim().length() > 0) {
            pathways.add(subSystem.toLowerCase());
          }
        }
      }

      // reading the kegg pathways table
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[1]), "UTF8"));
      String line;
      
      // We could use a java 8 construct but not really necessary now
      // List<String> fileAsByteArray = Files.readAllLines(FileSystems.getDefault().getPath(filePath), Charset.forName("UTF-8"));
      
      while ((line = in.readLine()) != null) {
        if (line.trim().length() > 0 && !line.startsWith("kegg")) {
          String[] tokens = line.split("\t");

          keggPathways.put(tokens[1].toLowerCase(), tokens[0]);
        }
      }
      in.close();

      // reading the self annotated pathways table
      in = new BufferedReader(new InputStreamReader(new FileInputStream(args[2]), "UTF8"));
      
      while ((line = in.readLine()) != null) {
        if (line.trim().length() > 0 && !line.startsWith("pathway_name")) {
          String[] tokens = line.split("\t");
          String name = tokens[0].toLowerCase();
          String id = tokens[1];
          
          if ((id == null || id.trim().length() == 0) && (tokens.length >= 3)) {
            id = tokens[2];
          }
          
          keggPathways.put(name, id);
        }
      }
      in.close();
      
      
      int nbNotFound = 0;
      Set<String> unknownPathwayList = new HashSet<String>(); 
      Set<String> writtenPathways = new HashSet<String>(); 
      
      PrintWriter out = new PrintWriter("/home/rodrigue/data/wormReconstructions/2018-03-14-WormJam-GEM/wormjam-pathways.tsv");

      
      out.println("pathway_name\tgo\tkegg.pathway_id\tbiocic\tpw");

      for (String id : pathways) {

        if (writtenPathways.contains(id)) {
          continue;
        }

        String annotationId = keggPathways.get(id);

        if (annotationId != null) 
        {
          
          if (annotationId.startsWith("GO:")) {
            out.println(id + "\t" + annotationId);
          } else if (annotationId.startsWith("META:")) {
            out.println(id + "\t\t\t" + annotationId);
          } else if (annotationId.startsWith("PW:")) {
            out.println(id + "\t\t\t\t" + annotationId);
          }
          else {
            // kegg
            out.println(id + "\t\t" + annotationId);
          }
          
          writtenPathways.add(id);

        }
        else if (!id.contains(",")) 
        {
          System.out.println("Did not find a kegg pathway id for '" + id + "'");
          nbNotFound++;
          unknownPathwayList.add(id);
          out.println(id + "\t\t" + "");
          
          writtenPathways.add(id);
        }
        else 
        {
          String[] tokens = id.split(",");
          String potentialKeggName = "";
          int i = 0;
          
          for (String token : tokens) {

            if (potentialKeggName.trim().length() > 0 && i > 0 && i < (tokens.length)) {
              potentialKeggName += ", ";
            }
            
            potentialKeggName += token.trim();
            potentialKeggName = potentialKeggName.trim();
            
            System.out.println("\nTrying to find a kegg pathway id for '" + potentialKeggName + "'");

            if (keggPathways.get(potentialKeggName.trim()) != null) {
              System.out.println("\nFound");

              if (!writtenPathways.contains(potentialKeggName)) {
                annotationId = keggPathways.get(potentialKeggName);

                if (annotationId.startsWith("GO:")) {
                  out.println(potentialKeggName + "\t" + annotationId);
                } else if (annotationId.startsWith("META:")) {
                  out.println(potentialKeggName + "\t\t\t" + annotationId);
                } else if (annotationId.startsWith("PW:")) {
                  out.println(potentialKeggName + "\t\t\t\t" + annotationId);
                }
                else {
                  // kegg
                  out.println(potentialKeggName + "\t\t" + annotationId);
                }

                writtenPathways.add(potentialKeggName);
              }

              potentialKeggName = "";
            }
            
            i++;
          }
          
          if (potentialKeggName.length() > 0) {
            System.out.println("Did not find a kegg pathway id for '" + potentialKeggName + "'");
            nbNotFound++;
            unknownPathwayList.add(potentialKeggName);
            out.println(potentialKeggName);
            
            writtenPathways.add(id);
          }
        }
        
      }

      System.out.println("Number of unknown pathway = " + unknownPathwayList.size() + " / " + pathways.size() + "\n\n Unknowns:");

      Set<String> unknownPathwaywithComaList = new HashSet<String>(); 
      
      for (String p : unknownPathwayList) {
        if (!p.contains(",")) {
          System.out.println(p);
        } else {
          unknownPathwaywithComaList.add(p);
        }
      }
      System.out.println("\n");
      
      for (String p : unknownPathwaywithComaList) {
        System.out.println(p);
      }
      
      
      out.flush();
      out.close();
    } catch (XMLStreamException | IOException e) {
      e.printStackTrace();
    }

  }

}
