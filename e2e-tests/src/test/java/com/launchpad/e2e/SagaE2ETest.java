package com.launchpad.e2e;

import static io.restassured.config.JsonConfig.jsonConfig;
import static io.restassured.path.json.config.JsonPathConfig.NumberReturnType.BIG_DECIMAL;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SagaE2ETest {

    private static final String DEFAULT_GATEWAY_URL = "http://localhost:8081";

    private static final String APPLICATION_JSON = "application/json";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";
    private static final String BEARER = "Bearer ";

    private static final String AUTH_REGISTER_PATH = "/auth/register";
    private static final String AUTH_LOGIN_PATH = "/auth/login";
    private static final String CAMPAIGNS_PATH = "/campaigns";
    private static final String TRANSACTIONS_PATH = "/transactions";

    private static final int HTTP_OK = 200;
    private static final int HTTP_CREATED = 201;

    private static final String PASSWORD = "test";

    private static final String TRANSACTION_STATUS = "transactionStatus";
    private static final String CAMPAIGN_STATUS = "campaignStatus";
    private static final String TOKENS_SOLD = "tokensSold";
    private static final String TRANSACTION_ID = "transactionId";
    private static final String TOKEN = "token";
    private static final String CAMPAIGN_ID = "id";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_LIVE = "LIVE";

    private static final BigDecimal TRANSACTION_AMOUNT_SUCCESS = BigDecimal.valueOf(1000);
    private static final String TRANSACTION_AMOUNT_FAILURE = "999999";
    private static final String TARGET_AMOUNT = "5000";
    private static final BigDecimal ZERO_TOKENS_SOLD = BigDecimal.ZERO;

    private static final String TOKEN_NAME_PREFIX = "testToken";
    private static final String EMAIL_DOMAIN = "@gmail.com";

    private static final long POLL_INTERVAL_MS = 500;
    private static final long POLL_INTERVAL_CAMPAIGN_SECONDS = 1;
    private static final long TIMEOUT_SECONDS = 10;
    private static final long CAMPAIGN_START_DELAY_SECONDS = 5;

    private static final String TRANSACTION_REQUEST_TEMPLATE =
            "{\"campaignId\": \"%s\", \"amount\": \"%s\"}";

    private static final String CAMPAIGN_REQUEST_TEMPLATE =
            "{\"tokenName\": \"%s\", \"startTime\": \"%s\", \"targetAmount\": \"%s\"}";

    private static final String USER_REGISTRATION_REQUEST_TEMPLATE =
            "{\"email\": \"%s\", \"password\": \"%s\", \"repeatPassword\": \"%s\"}";

    private static final String USER_LOGIN_REQUEST_TEMPLATE =
            "{\"email\": \"%s\", \"password\": \"%s\"}";

    @BeforeAll
    public static void setup() {
        RestAssured.baseURI =
                System.getProperty("gateway.url", DEFAULT_GATEWAY_URL);
        RestAssured.config = RestAssured.config().jsonConfig(jsonConfig().numberReturnType(BIG_DECIMAL));
    }

    @Test
    public void SagaE2EHappyPath() {
        String token = createAndLoginNewUser();
        String campaignId = createNewCampaign(token);

        String transactionRequestBody =
                TRANSACTION_REQUEST_TEMPLATE.formatted(
                        campaignId,
                        TRANSACTION_AMOUNT_SUCCESS
                );

        String transactionId = RestAssured.given()
                .contentType(APPLICATION_JSON)
                .header(HEADER_AUTHORIZATION, BEARER + token)
                .header(HEADER_IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                .body(transactionRequestBody)
                .when()
                .post(TRANSACTIONS_PATH)
                .then()
                .statusCode(HTTP_CREATED)
                .body(TRANSACTION_STATUS, equalTo(STATUS_PENDING))
                .extract()
                .body()
                .jsonPath()
                .get(TRANSACTION_ID)
                .toString();

        await()
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String status = RestAssured.given()
                            .header(HEADER_AUTHORIZATION, BEARER + token)
                            .when()
                            .get(TRANSACTIONS_PATH + "/" + transactionId)
                            .then()
                            .statusCode(HTTP_OK)
                            .extract()
                            .body()
                            .jsonPath()
                            .getString(TRANSACTION_STATUS);

                    assertEquals(STATUS_COMPLETED, status);
                });

        await()
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    BigDecimal tokensSold = RestAssured.given()
                            .header(HEADER_AUTHORIZATION, BEARER + token)
                            .when()
                            .get(CAMPAIGNS_PATH + "/" + campaignId)
                            .then()
                            .statusCode(HTTP_OK)
                            .extract()
                            .body()
                            .jsonPath()
                            .get(TOKENS_SOLD);

                    assertEquals(0, TRANSACTION_AMOUNT_SUCCESS.compareTo(tokensSold));
                });
    }

    @Test
    public void SagaE2EFailed() {
        String token = createAndLoginNewUser();
        String campaignId = createNewCampaign(token);

        String transactionId = RestAssured.given()
                .contentType(APPLICATION_JSON)
                .header(HEADER_AUTHORIZATION, BEARER + token)
                .header(HEADER_IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                .body(
                        TRANSACTION_REQUEST_TEMPLATE.formatted(
                                campaignId,
                                TRANSACTION_AMOUNT_FAILURE
                        )
                )
                .when()
                .post(TRANSACTIONS_PATH)
                .then()
                .statusCode(HTTP_CREATED)
                .body(TRANSACTION_STATUS, equalTo(STATUS_PENDING))
                .extract()
                .body()
                .jsonPath()
                .get(TRANSACTION_ID)
                .toString();

        await()
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String status = RestAssured.given()
                            .header(HEADER_AUTHORIZATION, BEARER + token)
                            .when()
                            .get(TRANSACTIONS_PATH + "/" + transactionId)
                            .then()
                            .statusCode(HTTP_OK)
                            .extract()
                            .body()
                            .jsonPath()
                            .getString(TRANSACTION_STATUS);

                    assertEquals(STATUS_FAILED, status);
                });

        await()
                .pollInterval(POLL_INTERVAL_MS, TimeUnit.MILLISECONDS)
                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    BigDecimal tokensSold = RestAssured.given()
                            .header(HEADER_AUTHORIZATION, BEARER + token)
                            .when()
                            .get(CAMPAIGNS_PATH + "/" + campaignId)
                            .then()
                            .statusCode(HTTP_OK)
                            .extract()
                            .body()
                            .jsonPath()
                            .get(TOKENS_SOLD);

                    assertEquals(0, ZERO_TOKENS_SOLD.compareTo(tokensSold));
                });
    }

    private String createNewCampaign(String token) {
        String uniqueTokenName = TOKEN_NAME_PREFIX + LocalDateTime.now();
        String startTime = Instant.now()
                .plusSeconds(CAMPAIGN_START_DELAY_SECONDS)
                .toString();

        String requestBody = CAMPAIGN_REQUEST_TEMPLATE.formatted(
                uniqueTokenName,
                startTime,
                TARGET_AMOUNT
        );

        String campaignId = RestAssured.given()
                .contentType(APPLICATION_JSON)
                .header(HEADER_AUTHORIZATION, BEARER + token)
                .body(requestBody)
                .when()
                .post(CAMPAIGNS_PATH)
                .then()
                .statusCode(HTTP_CREATED)
                .extract()
                .body()
                .jsonPath()
                .get(CAMPAIGN_ID)
                .toString();

        await()
                .pollInterval(POLL_INTERVAL_CAMPAIGN_SECONDS, TimeUnit.SECONDS)
                .atMost(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String status = RestAssured.given()
                            .header(HEADER_AUTHORIZATION, BEARER + token)
                            .when()
                            .get(CAMPAIGNS_PATH + "/" + campaignId)
                            .then()
                            .statusCode(HTTP_OK)
                            .extract()
                            .body()
                            .jsonPath()
                            .get(CAMPAIGN_STATUS)
                            .toString();

                    assertEquals(STATUS_LIVE, status);
                });

        return campaignId;
    }

    private String createAndLoginNewUser() {
        String uniqueEmail = LocalDateTime.now() + EMAIL_DOMAIN;

        String userRequestBody =
                USER_REGISTRATION_REQUEST_TEMPLATE.formatted(uniqueEmail, PASSWORD, PASSWORD);

        RestAssured.given()
                .contentType(APPLICATION_JSON)
                .body(userRequestBody)
                .when()
                .post(AUTH_REGISTER_PATH)
                .then()
                .log().all()
                .statusCode(HTTP_CREATED);

        String loginRequestBody =
                USER_LOGIN_REQUEST_TEMPLATE.formatted(uniqueEmail, PASSWORD);

        return RestAssured.given()
                .contentType(APPLICATION_JSON)
                .body(loginRequestBody)
                .when()
                .post(AUTH_LOGIN_PATH)
                .then()
                .statusCode(HTTP_OK)
                .extract()
                .body()
                .jsonPath()
                .get(TOKEN)
                .toString();
    }
}
