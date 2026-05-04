package com.rith.ventro_bakong.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BakongPaymentResponse {

    @Schema(description = "QR code string to display")
    private String qrCode;

    @Schema(description = "MD5 hash used to verify payment")
    private String md5;

    @Schema(description = "Bill number")
    private String billNumber;

    @Schema(description = "Amount")
    private Double amount;

    @Schema(description = "Currency")
    private String currency;
}