package org.mitre.inferno.utils;

import org.hl7.fhir.r5.model.CodeableConcept;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.IntegerType;
import org.hl7.fhir.r5.model.OperationOutcome;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;
import org.hl7.fhir.r5.model.OperationOutcome.OperationOutcomeIssueComponent;

/**
 * Util class for end points
 */
public class EndpointUtils {
  
  /**
   * Convenience method to get an operation outcome
   * @param message the message for the oo
   * @param sev severity of the oo
   * @return the operation outcome
   */
  public static OperationOutcome getOperationOutcome(String message, OperationOutcome.IssueSeverity sev, int line, int col) {
    OperationOutcome oo = null;
    OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent(sev, IssueType.STRUCTURE);
    issue.setDiagnostics(message);
    issue.setDetails(new CodeableConcept().setText(message));
    issue.addExtension("http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-line", new IntegerType(line));
    issue.addExtension("http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-col", new IntegerType(col));
    issue.addExtension("http://hl7.org/fhir/StructureDefinition/operationoutcome-issue-source", new CodeType("ValidationService"));
    oo = new OperationOutcome(issue);
    return oo;
  }
  
}
