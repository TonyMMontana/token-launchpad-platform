package com.campaignservice.smoke;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class CampaignServiceSmokeTest {

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        String envUrl = System.getenv("SMOKE_TEST_URL");
        RestAssured.baseURI = (envUrl != null && !envUrl.isBlank()) ? envUrl : "http://localhost:8082";
    }

    @Test
    public void campaignServiceShouldBeReadable() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));

        given()
                .when()
                .get("/campaigns")
                .then()
                .statusCode(200);
    }
}
