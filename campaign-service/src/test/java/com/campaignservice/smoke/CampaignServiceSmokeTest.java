package com.campaignservice.smoke;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class CampaignServiceSmokeTest {

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        String envUrl = System.getenv("SMOKE_TEST_URL");
        RestAssured.baseURI = (envUrl != null && !envUrl.isBlank()) ? envUrl : "http://localhost:8080";
        RestAssured.basePath = "/campaigns";
    }

    @Test
    public void campaignHappyPathShouldCreateAndFetchCampaigns() {
        given()
                .when()
                .get()
                .then()
                .statusCode(200);
        String uniqueTokenName = "SmokeTest-" + System.currentTimeMillis();
        String uniqueStateTime = LocalDateTime.now().plusMinutes(5).toString();
        String requestBody = """
                {
                    "tokenName": "%s",
                    "targetAmount": 100000,
                    "startTime": "%s"
                }
                """.formatted(uniqueTokenName, uniqueStateTime);

        Integer newId = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post()
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");

        given()
                .pathParam("id", newId)
                .when()
                .get("/{id}")
                .then()
                .statusCode(200)
                .body("id", equalTo(newId))
                .body("tokenName", equalTo(uniqueTokenName));
    }
}
