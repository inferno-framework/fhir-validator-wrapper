package org.mitre.inferno;

import org.mitre.inferno.App;
import org.mitre.inferno.Validator;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
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



public class EndpointTest {
    final static String ROOT_URI = "https://localhost:4567";

    @BeforeAll
    public static void setup() {
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
    /* 
    @After
    public void setdown() throws Exception {
        Thread.sleep(1000);
        Spark.stop();
    }
    */

    @Test
    public void testAssured() throws IOException {
        RestAssured.given().when().get("/resources").then().statusCode(200);
    /*\
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();

        HttpGet httpGet = new HttpGet("http://localhost:4567");
        CloseableHttpResponse response = httpClient.execute(httpGet);

        int statusCode = response.getStatusLine().getStatusCode();
        BufferedReader rd = new BufferedReader(
                 new InputStreamReader(response.getEntity().getContent()));

        StringBuffer result = new StringBuffer();
        String line = "";
        while ((line = rd.readLine()) != null) {
            result.append(line);
        }

        assertEquals(200, statusCode);
        */
    }

    @Test
    public void resourceTest() {
        Logger logger = LoggerFactory.getLogger(App.class);
        RequestSpecification httpRequest = RestAssured.given();
	    Response response = httpRequest.get("/resources");
        ResponseBody body = response.getBody();
        System.out.println("Response Body is: " + body.asString());
        logger.info("Response Body is: " + body.asString());
	
    }


    @Test
    public void httpResourceTest() throws IOException {
        Logger logger = LoggerFactory.getLogger(App.class);
        try {
            Validator validator = new Validator("./igs");
            List<String> resources = validator.getResources();
            HttpRequest request = HttpRequest.newBuilder().uri(new URI(ROOT_URI + "/resources")).GET().build();
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
                logger.info(response.toString());
                logger.info(resources.toString());
                assertEquals(request.toString(), resources.toString());
        }catch (Exception e) {
            e.printStackTrace();
            fail();
          }
    }
    
 
}
