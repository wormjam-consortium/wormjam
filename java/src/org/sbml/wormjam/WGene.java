package org.sbml.wormjam;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("javadoc")
public class WGene {

  private String id;
  private String symbol;
  private String locus;
  private String name;
  private List<String> goProcess = new ArrayList<String>();
  private List<String> goFunction = new ArrayList<String>();
  private List<String> goComponent = new ArrayList<String>();
  private List<String> ecCode = new ArrayList<String>();
  /**
   * @return the id
   */
  public String getId() {
    return id;
  }
  /**
   * @param id the id to set
   */
  public void setId(String id) {
    this.id = id;
  }
  /**
   * @return the goProcess
   */
  public List<String> getGoProcess() {
    return goProcess;
  }
  /**
   * @return the goProcess
   */

  public String getGoProcessAsString() {
    String result = "";
    
    for (String goTerm : goProcess) {
      if (result.length() > 0) {
        result += ";";  
      }
      result += goTerm;
    }
    
    return result;
  }
  /**
   * @param goId the goProcess to set
   */
  public void addGoProcess(String goId) {
    if (!this.goProcess.contains(goId)) {
      this.goProcess.add(goId);
    }
  }
  /**
   * @return the goFunction
   */
  public List<String> getGoFunction() {
    return goFunction;
  }
  /**
   * @return the goFunction
   */
  public String getGoFunctionAsString() {
    String result = "";
    
    for (String goTerm : goFunction) {
      if (result.length() > 0) {
        result += ";";  
      }
      result += goTerm;
    }
    
    return result;
  }
  /**
   * @param goId the goFunction to set
   */
  public void addGoFunction(String goId) {
    if (!this.goFunction.contains(goId)) {
      this.goFunction.add(goId);
    }
  }
  /**
   * @return the goBiology
   */
  public List<String> getGoComponent() {
    return goComponent;
  }
  
  /**
   * @return the goBiology
   */
  public String getGoComponentAsString() {
    String result = "";
    
    for (String goTerm : goComponent) {
      if (result.length() > 0) {
        result += ";";  
      }
      result += goTerm;
    }
    
    return result;
  }
  
  /**
   * @param goId the goBiology to set
   */
  public void addGoComponent(String goId) {
    if (!this.goComponent.contains(goId)) {
      this.goComponent.add(goId);
    }
  }
  /**
   * @return the ecCode
   */
  public List<String> getEcCode() {
    return ecCode;
  }
  
  public String getEcCodeAsString() {
    String result = "";
    
    for (String ecId : ecCode) {
      if (result.length() > 0) {
        result += ";";  
      }
      result += ecId;
    }
    
    return result;
  }
  
  /**
   * @param ecCode the ecCode to set
   */
  public void addEcCode(String ecCode) {
    this.ecCode.add(ecCode);
  }
  /**
   * @return the symbol
   */
  public String getSymbol() {
    return symbol;
  }
  /**
   * @param symbol the symbol to set
   */
  public void setSymbol(String symbol) {
    this.symbol = symbol;
  }
  /**
   * @return the locus
   */
  public String getLocus() {
    return locus;
  }
  /**
   * @param locus the locus to set
   */
  public void setLocus(String locus) {
    this.locus = locus;
  }
  /**
   * @return the name
   */
  public String getName() {
    return name;
  }
  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }
  
  
  
}
