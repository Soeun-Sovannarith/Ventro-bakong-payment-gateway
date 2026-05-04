package com.rith.ventro_bakong.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BakongPaymentRequest {

    @Schema(description = "Order ID", example = "1")
    private Long orderId;

    @Schema(description = "Currency: USD or KHR", example = "USD")
    private String currency;
}
