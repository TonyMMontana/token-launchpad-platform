package com.campaignservice.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.campaignservice.dto.CampaignResponseDto;
import com.campaignservice.model.Campaign;
import com.campaignservice.repository.CampaignRepository;
import com.campaignservice.service.CampaignMessagingService;
import com.campaignservice.service.CampaignService;
import com.launchpad.common.event.ReserveTokensEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;

public class CampaignCachingIntegrationTest extends AbstractIntegrationTest {

    public static final String SAMPLE_TOKEN = "Sample Token";
    public static final String UPDATED_SAMPLE_TOKEN = "Updated Sample Token";

    @Autowired
    private CampaignRepository campaignRepository;

    @Autowired
    private CampaignService campaignService;

    @MockitoBean
    private CampaignMessagingService campaignMessagingService;

    @Test
    public void shouldReturnCachedDataUntilReserveTokensTriggersEviction() {
        Campaign campaign = new Campaign();
        campaign.setTokenName(SAMPLE_TOKEN);
        campaign.setCampaignStatus(Campaign.CampaignStatus.LIVE);
        campaign.setTargetAmount(BigDecimal.valueOf(1000));
        campaign.setTokensSold(BigDecimal.ZERO);

        campaignRepository.save(campaign);
        campaignService.getCampaign(campaign.getId()); //load cache

        campaign.setTokenName(UPDATED_SAMPLE_TOKEN);
        campaignRepository.save(campaign);

        CampaignResponseDto cachedResponseDto = campaignService.getCampaign(campaign.getId());

        assertEquals(
                SAMPLE_TOKEN,
                cachedResponseDto.tokenName(),
                "Cache proxy failed: The service read directly from the DB instead of returning the cached value.");

        campaignService.reserveTokens(new ReserveTokensEvent(1L, campaign.getId(), BigDecimal.TEN));
        CampaignResponseDto updatedResponseDto = campaignService.getCampaign(campaign.getId());

        assertEquals(
                UPDATED_SAMPLE_TOKEN,
                updatedResponseDto.tokenName(),
                "Cache eviction failed: The @CacheEvict annotation did not flush the stale Redis entry.");
    }
}
