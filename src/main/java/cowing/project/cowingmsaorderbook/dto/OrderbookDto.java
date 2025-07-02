package cowing.project.cowingmsaorderbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record OrderbookDto(
        @JsonProperty("type") String type,
        @JsonProperty("code") String code,
        @JsonProperty("timestamp") long timestamp,
        @JsonProperty("total_ask_size") BigDecimal totalAskSize,
        @JsonProperty("total_bid_size") BigDecimal totalBidSize,
        @JsonProperty("orderbook_units") List<OrderbookUnit> orderbookUnits
) {}
