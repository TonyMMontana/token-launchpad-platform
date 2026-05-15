package com.campaignservice.integration;

import com.campaignservice.repository.CampaignRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.cache.CacheManager;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Objects;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class AbstractIntegrationTest {

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CacheManager cacheManager;

    public static final String POSTGRES_16_ALPINE = "postgres:16-alpine";
    public static final String MASSTRANSIT_RABBITMQ_LATEST = "masstransit/rabbitmq:latest";
    public static final String REDIS_7_2_ALPINE = "redis:7.2-alpine";
    public static final int REDIS_PORT = 6379;

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_16_ALPINE);

    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer(
            DockerImageName
                    .parse(MASSTRANSIT_RABBITMQ_LATEST)
                    .asCompatibleSubstituteFor("rabbitmq")
    );

    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(REDIS_7_2_ALPINE)
            .withExposedPorts(REDIS_PORT);

    static {
        postgres.start();
        rabbit.start();
        redis.start();
    }

    @AfterEach
    protected void cleanUp() {
        campaignRepository.deleteAll();

        for (String cacheName : cacheManager.getCacheNames()) {
            Objects.requireNonNull(cacheManager.getCache(cacheName)).clear();
        }
    }
}