package org.mitre.inferno;

import org.mitre.inferno.App;
import org.mitre.inferno.Validator;
import org.mitre.inferno.rest.IgResponse;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.BufferedReader;
import org.apache.commons.io.IOUtils;
import java.net.URL;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import io.restassured.RestAssured;
import io.restassured.http.Method;
import io.restassured.response.Response;
import io.restassured.response.ResponseBody;
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents a tester for api endpoints.
 */
public class EndpointTest {
    private Logger logger = LoggerFactory.getLogger(App.class);
    private static Validator validator;

    @BeforeAll
    public static void setup() throws Exception {
        // get validation results directly from a validator instance
        try {
            validator = new Validator("./igs");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // start the wrapper app
        String [] args = {};
        App.main(args);
        // set up restAssured for tests that center its use
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
            // api and validator responses
            List<String> resources = this.validator.getResources();
            RequestSpecification httpRequest = RestAssured.given();
            Response response = httpRequest.get("/resources");
            ResponseBody body = response.getBody();
            // convert to json for comparison
            JsonArray fromAPI = new Gson().fromJson(body.asString(), JsonArray.class);
            JsonArray fromValidator = new Gson().fromJson(resources.toString(), JsonArray.class);
            // test against resources directly from the validator
            assertEquals(fromAPI.toString(), fromValidator.toString());
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
    }
    
    @Test
    public void validateResourceBasicURL() throws IOException{
        // api responses 
        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/validate");
        ResponseBody body = response.getBody();
        // process the response
        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();
        // test response
        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }
    
    @Test
    public void validateResourceBasicURLWithType() throws IOException{
        // api responses 
        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/Patient/validate");
        ResponseBody body = response.getBody();
        // process the response
        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();
        // test response
        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }
    
    @Test
    public void validateResourceWithType() throws IOException{
        // api responses 
        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("/Patient/$validate");
        ResponseBody body = response.getBody();
        // process the response
        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();
        // test response
        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }

    @Test
    public void validateResourceWithID() throws IOException{
        // api responses 
        JsonObject jsonObj = loadFile("src/test/resources/us_core_patient_example.json");
        String exampleResource = new Gson().toJson(jsonObj);
        RequestSpecification httpRequest = RestAssured.given();
        Response response = httpRequest.body(exampleResource).when().post("Patient/123/$validate");
        ResponseBody body = response.getBody();
        // process the response
        JsonObject fromAPI = new Gson().fromJson(body.asString(), JsonObject.class);
        String validation = JsonParser.parseString(body.asString()).getAsJsonObject().getAsJsonArray("issue").get(0).getAsJsonObject().get("details").getAsJsonObject().get("text").getAsString();
        // test response
        assertTrue(fromAPI.has("resourceType"));
        assertEquals("All OK", validation);
    }
    
    JsonObject loadFile (String fileName) throws FileNotFoundException, IOException{
        BufferedReader br = new BufferedReader(new FileReader(fileName));
        JsonObject obj = JsonParser.parseReader(br).getAsJsonObject();
        return obj;
    }
}
