package com.campaignservice.integration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campaignservice.config.RabbitMQConfig;
import com.campaignservice.event.ReserveTokensEvent;
import com.campaignservice.model.Campaign;
import com.campaignservice.model.Status;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.service.CampaignMessagingService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class CampaignMessagingIntegrationTest extends AbstractIntegrationTest {

    public static final String SAMPLE_TOKEN = "Sample Token";
    public static final int MILLIS = 200;
    public static final int TIMEOUT = 10;
    public static final long TRANSACTION_ID = 1L;
    public static final int TARGET_AMOUNT = 1000;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CampaignRepository campaignRepository;

    @MockitoBean
    private CampaignMessagingService campaignMessagingService;

    @Test
    public void shouldConsumeRabbitMessagesAndUpdateCampaign() {
        Campaign campaign = new Campaign();
        campaign.setTokenName(SAMPLE_TOKEN);
        campaign.setTargetAmount(BigDecimal.valueOf(TARGET_AMOUNT));
        campaign.setTokensSold(BigDecimal.ZERO);
        campaign.setStatus(Status.LIVE);
        campaignRepository.save(campaign);

        ReserveTokensEvent event = new ReserveTokensEvent(TRANSACTION_ID, campaign.getId(), BigDecimal.TEN);
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_SAGA, RabbitMQConfig.ROUTING_RESERVE, event);

        await()
                .atMost(TIMEOUT, TimeUnit.SECONDS)
                .pollInterval(Duration.ofMillis(MILLIS))
                .untilAsserted(() ->
                {
                    Campaign updated = campaignRepository
                            .findById(campaign.getId())
                            .orElseThrow();

                    assertEquals(
                            0,
                            updated.getTokensSold().compareTo(BigDecimal.TEN),
                            "Tokens sold should evaluate mathematically to 10"
                    );
                });
    }
}
