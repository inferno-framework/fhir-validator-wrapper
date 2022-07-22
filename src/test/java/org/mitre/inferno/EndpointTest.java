package org.mitre.inferno;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;


/**
 * Represents a tester for api endpoints.
 */
public class EndpointTest {
    private static Validator validator;

    @BeforeAll
    public static void setup() throws Exception {
        try {
            validator = new Validator("./igs");
        } catch (Exception e) {
            e.printStackTrace();
        }
        String [] args = {};
        App.main(args);
        String port = System.getProperty("server.port");
        if (port == null) {
            RestAssured.port = Integer.valueOf(4567);
        }
        else{
            RestAssured.port = Integer.valueOf(port);
        }
        String baseHost = System.getProperty("server.host");
        if(baseHost==null){
            baseHost = "http://localhost";
        }
        RestAssured.baseURI = baseHost;
    }
    
    @Test
    public void getResourceTest() throws Exception{
        try {
            List<String> resources = EndpointTest.validator.getResources();
            RequestSpecification httpRequest = RestAssured.given();
            Response response = httpRequest.get("/resources");
            ResponseBody body = response.getBody();

            JsonArray fromAPI = new Gson().fromJson(body.asString(), JsonArray.class);
            JsonArray fromValidator = new Gson().fromJson(resources.toString(), JsonArray.class);
            assertEquals(fromAPI.toString(), fromValidator.toString());
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void validateResourceBasicURL() throws IOException{
        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/validate");
        ResponseBody body = response.getBody();

        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();

        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }
    
    @Test
    public void validateResourceWithType() throws IOException{

        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/Patient/$validate");
        ResponseBody body = response.getBody();

        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();

        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }

    @Test
    public void validateResourceWithID() throws IOException{

        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("Patient/12345/$validate");
        ResponseBody body = response.getBody();

        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();

        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }

    @Test
    public void validateResourceBasicURLWrongProfile() throws IOException{

        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/validate?profile=http://hl7.org/fhir/StructureDefinition/Claim");
        ResponseBody body = response.getBody();

        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("severity").getAsString();

        assertTrue(fromAPI.has("resourceType"));
        assertEquals("error", validation);
    }

    @Test
    public void validateResourceWithTypeWrongProfile() throws IOException{

        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/Patient/$validate?profile=http://hl7.org/fhir/StructureDefinition/Claim");
        ResponseBody body = response.getBody();

        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("severity").getAsString();

        assertTrue(fromAPI.has("resourceType"));
        assertEquals("error", validation);
    }

    @Test
    public void validateResourceWithIDWrongProfile() throws IOException{

        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/Patient/12345/$validate?profile=http://hl7.org/fhir/StructureDefinition/Claim");
        ResponseBody body = response.getBody();

        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("severity").getAsString();
 
        assertTrue(fromAPI.has("resourceType"));
        assertEquals("error", validation);
    }

    JsonObject loadFile (String fileName) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        JsonObject obj = JsonParser.parseReader(br).getAsJsonObject();
        return obj;
    }
}
