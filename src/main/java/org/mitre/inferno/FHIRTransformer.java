package org.mitre.inferno;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import spark.Response;
import spark.Request;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.OperationOutcome;

import org.mitre.inferno.utils.EndpointUtils;

/**
 * FHIR Transformer class that deals with all XML<>JSON transformations
 * It is assumed we are only going to be dealing with FHIR R3, R4 & R5
 */
public class FHIRTransformer {

  private final FhirContext contextR3;
  private final IParser jsonParserR3;
  private final IParser xmlParserR3;

  private final FhirContext contextR4;
  private final IParser jsonParserR4;
  private final IParser xmlParserR4;

  private final FhirContext contextR5;
  private final IParser jsonParserR5;
  private final IParser xmlParserR5;
  
  public static enum supportedFHIRVersions { R3, R4, R5 };

  /**
   * FHIRTransformer()
   */
  public FHIRTransformer() {

      //create the necessary hapi context & related parsers
      this.contextR3 = FhirContext.forDstu3();
      this.jsonParserR3 = contextR3.newJsonParser();
      this.xmlParserR3 = contextR3.newXmlParser();
      
      this.contextR4 = FhirContext.forR4();
      this.jsonParserR4 = contextR4.newJsonParser();
      this.xmlParserR4 = contextR4.newXmlParser();

      this.contextR5 = FhirContext.forR5();
      this.jsonParserR5 = contextR5.newJsonParser();
      this.xmlParserR5 = contextR5.newXmlParser();

  }

  /**
   * Handles transforming resources between json<>xml
   *
   * @param resource  the resource to be transformed
   * @param req the request of the transform
   * @param res
   * @return the validation res
   * @throws Exception if the resource cannot be loaded or validated
   */
  public String transformResource(String resource, Request req , Response res) throws Exception {

    String direction = req.queryParams("direction");
    String fhir_version = req.queryParams("fhir_version");

    res.type((((new String("JSON2XML")).compareTo(((direction==null)?"":direction)) == 0) ? "application/fhir+xml" : "application/fhir+json"));

    String transformedContent = null;
    int line = 1, col = 1; //defaults
    
    direction = ((direction==null)?"":direction);
    fhir_version = ((fhir_version==null)?"":fhir_version);


    if( !((((new String("JSON2XML")).compareTo(direction)) == 0) || (((new String("XML2JSON")).compareTo(direction)) == 0)) ) {
      String message = "Missing or Invalid direction parameter : 'direction', needs to be one JSON2XML or XML2JSON.";
      res.type("application/fhir+json");
      return new JsonParser().composeString(EndpointUtils.getOperationOutcome(message, OperationOutcome.IssueSeverity.ERROR, line, col));
    }    

    if( !(((supportedFHIRVersions.R3.toString().compareTo(fhir_version)) == 0) || 
          ((supportedFHIRVersions.R4.toString().compareTo(fhir_version)) == 0) || 
          ((supportedFHIRVersions.R5.toString().compareTo(fhir_version)) == 0)) ) {

      String message = "Missing or Invalid direction parameter : 'fhir_version', needs to be one of R3, R4, or R5.";      
      res.type("application/fhir+json");
      return new JsonParser().composeString(EndpointUtils.getOperationOutcome(message, OperationOutcome.IssueSeverity.ERROR, line, col));
    }

    try {
      transformedContent = ((((new String("JSON2XML")).compareTo(direction)) == 0) ? convertJsonToXmlFhir(resource, fhir_version) : convertXmlToJsonFhir(resource, fhir_version));            
    } catch(DataFormatException dfe) {
      String message = "ACK! Transformation error : "+dfe.getMessage();      
      res.type("application/fhir+json");
      return new JsonParser().composeString(EndpointUtils.getOperationOutcome(message, OperationOutcome.IssueSeverity.ERROR, line, col));
    }

    if(transformedContent == null) {
      String message = "ACK! Transform failed for the direction : " + direction+" and FHIR version:" + fhir_version;
      res.type("application/fhir+json");
      return new JsonParser().composeString(EndpointUtils.getOperationOutcome(message, OperationOutcome.IssueSeverity.ERROR, line, col));
    }

    return transformedContent;
  }

  /**
   * Handles transform of Json To Xml Fhir
   * 
   * @param content the resource content to transform
   * @param fhir_version the fhir version we are working with {R3,R4,R5}
   * @return the transformed xml resource
   * @throws DataFormatException
   */
    private String convertJsonToXmlFhir(final String content, final String fhir_version) throws DataFormatException {
        switch(fhir_version) { // output XML
          case "R3":
            return convertSourceToTargetFhir(content, jsonParserR3, xmlParserR3); 
          case "R4":
            return convertSourceToTargetFhir(content, jsonParserR4, xmlParserR4);
          case "R5":
            return convertSourceToTargetFhir(content, jsonParserR5, xmlParserR5); 
        }
        return null;   
    }

  /**
   *  Handles transform of Xml To Json Fhir
   * 
   * @param content the resource content to transform
   * @param fhir_version the fhir version we are working with {R3,R4,R5}
   * @return the transformed json resource
   * @throws DataFormatException
   */
    private String convertXmlToJsonFhir(final String content, final String fhir_version) throws DataFormatException {
        switch(fhir_version) { // output JSON
          case "R3":
            return convertSourceToTargetFhir(content,xmlParserR3,jsonParserR3); 
          case "R4":
            return convertSourceToTargetFhir(content,xmlParserR4,jsonParserR4); 
          case "R5":
            return convertSourceToTargetFhir(content,xmlParserR5,jsonParserR5); 
        }
        return null;
    }

    /**
     * Handles the generic transform from source format to target format
     * 
     * @param content the resource content to transform
     * @param source the source parser
     * @param target the target parser 
     * @return the transformed resource
     * @throws DataFormatException
     */
    private String convertSourceToTargetFhir(final String content, IParser source, IParser target) throws DataFormatException {
        IBaseResource resource = source.parseResource(content); // parse the resource
        return target.setPrettyPrint(true).encodeResourceToString(resource); // output in target format
    }

}
