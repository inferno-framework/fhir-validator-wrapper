package org.mitre.inferno;

import java.io.IOException;
import java.util.Date;

import org.hl7.fhir.r5.formats.JsonParser;
import org.hl7.fhir.r5.model.CapabilityStatement;
import org.hl7.fhir.r5.model.Enumerations;
import org.hl7.fhir.r5.model.Enumerations.CapabilityStatementKind;
import org.hl7.fhir.r5.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r5.model.ResourceType;
import org.hl7.fhir.r5.model.Enumerations.PublicationStatus;

public class CapabilityStatementGenerator {
    private static final long lastChangedDate = 1617031380000L;
    private static CapabilityStatement capabilityStatement = null;

    public static String generateCapabilityStatement() throws IOException {
        // Only build a new CapabilityStatement object if one hasn't been built yet
        if (capabilityStatement == null) {
            capabilityStatement = new CapabilityStatement(
                    PublicationStatus.ACTIVE,
                    new Date(lastChangedDate),
                    CapabilityStatementKind.INSTANCE,
                    FHIRVersion._4_0_1,
                    "json"
            );
            capabilityStatement.addFormat("xml");

            // Add a "software" section to let people know what this is an instance of
            CapabilityStatement.CapabilityStatementSoftwareComponent softwareComponent = new CapabilityStatement.CapabilityStatementSoftwareComponent("Inferno Validation Service");
            softwareComponent.setVersion(Version.getVersion());
            capabilityStatement.setSoftware(softwareComponent);

            // Add an "instance" section which describes this instance of the validator
            CapabilityStatement.CapabilityStatementImplementationComponent implementationComponent = new CapabilityStatement.CapabilityStatementImplementationComponent();
            implementationComponent.setDescription("Inferno Resource Validator");
            capabilityStatement.setImplementation(implementationComponent);

            // Add a "rest" component, which declares that you can use this to validate against every resource type
            CapabilityStatement.CapabilityStatementRestComponent restComponent = new CapabilityStatement.CapabilityStatementRestComponent(
                    Enumerations.RestfulCapabilityMode.SERVER
            );
            CapabilityStatement.CapabilityStatementRestResourceOperationComponent validateOp = new CapabilityStatement.CapabilityStatementRestResourceOperationComponent(
                    "validate",
                    "http://hl7.org/fhir/OperationDefinition/Resource-validate"
            );
            for (ResourceType type : ResourceType.values()) {
                CapabilityStatement.CapabilityStatementRestResourceComponent resourceComponent = new CapabilityStatement.CapabilityStatementRestResourceComponent();
                resourceComponent.setType(type.name());
                resourceComponent.addOperation(validateOp);
                restComponent.addResource(resourceComponent);
            }
            capabilityStatement.addRest(restComponent);
        }

        return new JsonParser().composeString(capabilityStatement);
    }
}
