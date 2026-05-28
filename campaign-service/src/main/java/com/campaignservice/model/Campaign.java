package com.campaignservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@Table(name = "campaigns")
public class Campaign {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String tokenName;
    BigDecimal targetAmount;
    BigDecimal tokensSold;
    LocalDateTime startTime;
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    CampaignStatus campaignStatus;

    public enum CampaignStatus {
        PENDING,
        LIVE,
        SUCCESS,
        FAILED
    }
}
