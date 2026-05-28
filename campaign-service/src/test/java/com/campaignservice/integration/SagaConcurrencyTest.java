package com.campaignservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campaignservice.model.Campaign;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.service.CampaignMessagingService;
import com.campaignservice.service.CampaignService;
import com.launchpad.common.event.ReserveTokensEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SagaConcurrencyTest extends AbstractIntegrationTest {
    public static final int TARGET_SUPPLY = 100;
    public static final int SOLD = 90;
    public static final int THREAD_COUNT = 50;
    public static final int COUNT = 1;
    public static final int TIMEOUT = 10;

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignService campaignService;

    @MockitoBean
    private CampaignMessagingService campaignMessagingService;

    @Test
    public void shouldPreventOversellingUnderHighConcurrency() throws InterruptedException {
        Campaign campaign = new Campaign();
        campaign.setTargetAmount(BigDecimal.valueOf(TARGET_SUPPLY));
        campaign.setTokensSold(BigDecimal.valueOf(SOLD));
        campaign.setCampaignStatus(Campaign.CampaignStatus.LIVE);
        Campaign saved = campaignRepository.save(campaign);

        final Long targetCampaignId = saved.getId();

        int threadCount = THREAD_COUNT;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch countDownLatch = new CountDownLatch(COUNT);

        for (int i = 0; i < threadCount; i++) {
            final long uniqueTransactionId = i;
            executorService.submit(() -> {
                try {
                    countDownLatch.await();
                    campaignService.reserveTokens(new ReserveTokensEvent(uniqueTransactionId, targetCampaignId, BigDecimal.ONE));
                } catch (Exception ignored) {
                }
            });
        }
        countDownLatch.countDown();
        executorService.shutdown();
        executorService.awaitTermination(TIMEOUT, TimeUnit.SECONDS);

        Campaign updatedCampaign = campaignRepository.findById(campaign.getId()).orElseThrow();
        assertEquals(
                0,
                updatedCampaign.getTokensSold().compareTo(BigDecimal.valueOf(TARGET_SUPPLY)),
                "Inventory protection failed: Final tokens sold must exactly equal the target capacity limit");
    }
}
