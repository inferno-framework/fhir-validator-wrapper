package org.mitre.inferno;

import org.mitre.inferno.App;
import org.mitre.inferno.Validator;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
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
import io.restassured.specification.RequestSpecification;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represents a tester for api endpoints.
 */
public class EndpointTest {
    private final static String ROOT_URI = "http://localhost:4567";
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
    /* 
    @Test
    public void testAssured() throws IOException {
        RestAssured.given().when().get("/resources").then().statusCode(200);
    
    }
    */
    
    @Test
    public void resourceTest() {
        try {
            // api and validator responses
            List<String> resources = this.validator.getResources();
            RequestSpecification httpRequest = RestAssured.given();
            Response response = httpRequest.get("/resources");
            ResponseBody body = response.getBody();
            // convert same to json format
            JsonArray fromAPI = new Gson().fromJson(body.asString(), JsonArray.class);
            JsonArray fromValidator = new Gson().fromJson(resources.toString(), JsonArray.class);
            // test
            System.out.println("Response Body is: " + fromAPI.toString());
            logger.info("Resource Body is: " + fromValidator.toString());
            System.out.println("Response Body is: " + body.asString());
            logger.info("Resource Body is: " + resources.toString());
            assertEquals(fromAPI.toString(), fromValidator.toString());
        } catch (Exception e){
            e.printStackTrace();
            fail();
        }
	
    }
    /* 
    @Test
    public void httpResourceTest() throws IOException {
        try {
            List<String> resources = this.validator.getResources();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(ROOT_URI + "/resources")).GET().build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                logger.info("Response Body is: " + response.toString());
                logger.info("Resource Body is: " + resources.toString());
                assertEquals(response.toString(), resources.toString());
        }catch (Exception e) {
            e.printStackTrace();
            fail();
          }
    }
    */
}
