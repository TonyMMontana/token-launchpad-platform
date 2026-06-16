package com.transactionservice.smoke;

import static com.launchpad.common.header.InternalHeaders.USER_ID_HEADER;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

public class TransactionServiceSmokeTest {

    @BeforeAll
    static void setUpBeforeClass() throws Exception {
        String envUrl = System.getenv("SMOKE_TEST_URL");
        RestAssured.baseURI = (envUrl != null && !envUrl.isBlank()) ? envUrl : "http://localhost:8083";
    }

    @Test
    public void transactionServiceShouldBeReadable() {
        given()
                .when()
                .get("/actuator/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));

        String testUserId = UUID.randomUUID().toString();

        given()
                .header(USER_ID_HEADER, testUserId)
                .when()
                .get("/transactions")
                .then()
                .body("totalElements", equalTo(0))
                .statusCode(200);
    }
}
