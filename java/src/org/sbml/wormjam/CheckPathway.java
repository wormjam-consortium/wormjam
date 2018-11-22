package org.sbml.wormjam;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * 
 * 
 * @author rodrigue
 *
 */
public class CheckPathway {

  
  /**
   * 
   */
  private static Set<String> pathwaySet = new HashSet<String>();
  
  
  /**
   * Compare the pathway list from Chintan and ours (All good).
   * 
   * @param args program arguments
   */
  public static void main(String[] args) 
  {
    if (args.length < 2) {
      System.out.println("We expect to get at least two arguments that point to text files with pathways entries.");
      System.exit(0);
    }
    
    try {
      //
      // reading the chintan pathways table
      //
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(args[0]), "UTF8"));
      String line;

      while ((line = in.readLine()) != null) {
        if (line.trim().length() > 0 && !line.startsWith("id")) {
          String[] tokens = line.split("\t");

          if (tokens.length >= 2) {
            String pathwayString = tokens[1];

            String[] pathwayNames = pathwayString.split(";");

            for (String pathwayName : pathwayNames) {
              pathwaySet.add(pathwayName.trim().toLowerCase());
            }
          }
        }
      }
      in.close();
      
      System.out.println("Found " + pathwaySet.size() + " pathway names in Chintan list.\n");

      //
      // reading the wormjam pathways table
      //
      in = new BufferedReader(new InputStreamReader(new FileInputStream(args[1]), "UTF8"));
      Set<String> unknownPathwayList = new HashSet<String>(); 
      int notFound = 0;
      
      while ((line = in.readLine()) != null) {
        if (line.trim().length() > 0 && !line.startsWith("pathway_name")) {
          String[] tokens = line.split("\t");

          String pathwayName = tokens[0].trim();
          
          if (! pathwaySet.contains(pathwayName)) {
            notFound++;
            System.out.println("Did not find '" + pathwayName + "'");
            unknownPathwayList.add(pathwayName);
          }
        }
      }
      in.close();

      
      System.out.println("Number of unknown pathway = " + unknownPathwayList.size() + " / " + pathwaySet.size() + "\n\n Unknowns:");

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
      
      
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

}
