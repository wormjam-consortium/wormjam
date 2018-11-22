package org.sbml.wormjam;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * 
 * @author rodrigue
 *
 */
public class WormBaseGeneTableImprovment {

  /**
   * @param args the arguments
   */
  public static void main(String[] args) {

    Map<String, WGene> wormGeneAnnotations = new HashMap<String, WGene>();
    
    // Reading the gene annotations table
    String geneAnnotationTableFileName = args[1];
    
    try{
      // Open the file
      GZIPInputStream fstream = new GZIPInputStream(new FileInputStream(geneAnnotationTableFileName));

      
      // File content:
      // WB      WBGene00000018  abl-1           GO:0004715      GO_REF:0000003  IEA     EC:2.7.10.2     F               M79.1   gene    taxon:6239      20170916        UniProt
      // 
      // id index = 1, GO index = 3, EC index = 6, GO branch index = 7
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
        
        if (tokens.length >= 12) {
          
          String id = tokens[1];
          String go = null;
          String ec = null;
          String goBranch = null;
          WGene wormGene = wormGeneAnnotations.get(id); 
              
          if (wormGene == null) {
            wormGene = new WGene();
            wormGene.setId(id);
            wormGeneAnnotations.put(id, wormGene);
          }
          
          if (tokens.length > 4 && tokens[4].trim().length() > 0) {
             go = tokens[4].trim();
          }
          if (tokens.length > 7 && tokens[7].trim().length() > 0) {
            ec = tokens[7].trim();
          }
          if (tokens.length > 8 && tokens[8].trim().length() > 0) {
            goBranch = tokens[8].trim();
          }
          
          if (ec != null && ec.startsWith("EC:")) {
            wormGene.addEcCode(ec);
          }
          if (go != null && goBranch != null && goBranch.equals("P")) {
            wormGene.addGoProcess(go);
            
          }
          if (go != null && goBranch != null && goBranch.equals("F")) {
            wormGene.addGoFunction(go);
            
          }
          if (go != null && goBranch != null && goBranch.equals("C")) {
            wormGene.addGoComponent(go);
            
          }
          
          
        } else {
          System.out.println("Process gene annotations - problem found " + tokens.length + " tokens.\n" + strLine);
        }
      }
      
      System.out.println("Read annotations for " + wormGeneAnnotations.size() + " entities");
      
      
      // System.out.println("\n\n" + wormGeneAnnotations.keySet());
      // System.exit(1);
      
      //Close the input stream
      in.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }

    
    // TODO - read the gene SBtab.tsv and insert what we can find from wormbase
    String geneTableFileName = args[0];
    
    try{
      // Open the file
      FileInputStream fstream = new FileInputStream(geneTableFileName);

      // output file
      PrintWriter out = new PrintWriter("/bi/group/compneur/worm/wormReconstructions/2017-12-05-WormJam-MunichWorkshop/WormJam-20171205_gene_GO.tsv");
      out.println("!!SBtab SbtabVersion='1.0' TableType='Gene' TableName='C elegans genes'");
      out.println("!ID\t!Identifiers:WormBase\t!Symbol\t!Locus\t!Name\t!GO_process\t!GO_function\t!GO_component\t!Identifiers:ec-code\t!Comment\t!Curator");
      
      // File content:
      // !ID     !Identifiers:WormBase   !Symbol !Locus  !Name   !Comment        !Curator
      // 
      //
      
      // Get the object of DataInputStream
      DataInputStream in = new DataInputStream(fstream);
      BufferedReader br = new BufferedReader(new InputStreamReader(in));
      String strLine;
      int nbLines = 0;
      int nbFound = 0;
      
      //Read File Line By Line
      while ((strLine = br.readLine()) != null)   {
        // Print the content on the console
        // System.out.println (strLine);
        
        // removing the header line
        if (strLine.startsWith("#") || strLine.startsWith("!")) {
          continue;
        }
        
        String[] tokens = strLine.split("\t");
        nbLines++;
        
        if (tokens.length >= 2) {
          
          String id = tokens[0].trim();
          String wormBaseId = tokens[1].trim();
          String symbol = null;
          String locus = null;
          String name = null;
          
          if (tokens.length > 2 && tokens[2].trim().length() > 0) {
            symbol = tokens[2].trim();
          }
          if (tokens.length > 3 && tokens[3].trim().length() > 0) {
            locus = tokens[3].trim();
          }
          if (tokens.length > 4 && tokens[4].trim().length() > 0) {
            name = tokens[3].trim();
          }
          
          out.print(id + "\t" + wormBaseId + "\t" + (symbol == null ? "" : symbol) + "\t" + (locus == null ? "" : locus) + "\t" + (name == null ? "" : name));
          
          WGene wormGene = wormGeneAnnotations.get(wormBaseId);
          
          if (wormGene != null) {
            // !Identifiers:go  !GO_function    !GO_biology !Identifiers:ec-code
            String goP = wormGene.getGoProcessAsString();
            String goF = wormGene.getGoFunctionAsString();
            String goB = wormGene.getGoComponentAsString();
            String ec = wormGene.getEcCodeAsString();
            nbFound++;
            
            // System.out.println("P - " + goP + ", F - " + goF + ", B - " + goB + ", EC = " + ec);
            
            out.print("\t" + (goP == null ? "" : goP) + "\t" + (goF == null ? "" : goF) + "\t" + (goB == null ? "" : goB) + "\t" + (ec == null ? "" : ec));
          } else {
            // System.out.println("Process genes - no annotations for '" + id + "'.");
          }
          
          // comment and curator - at the moment, there are always empty
          out.println("\t\t");
          
        } else {
          System.out.println("Process genes - problem found " + tokens.length + " tokens.");
        }
      }
      
      System.out.println("Found " + nbFound + " / " + nbLines);
      
      //Close the input stream
      in.close();
      out.flush();
      out.close();
    } catch (Exception e){//Catch exception if any
      System.err.println("Error: " + e.getMessage());
      e.printStackTrace();
    }

  }

}
