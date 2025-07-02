package cowing.project.cowingmsaorderbook.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record OrderbookUnit(
        @JsonProperty("ask_price") BigDecimal askPrice,
        @JsonProperty("bid_price") BigDecimal bidPrice,
        @JsonProperty("ask_size") BigDecimal askSize,
        @JsonProperty("bid_size") BigDecimal bidSize
) {}