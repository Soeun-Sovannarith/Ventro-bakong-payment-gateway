package com.rith.ventro_bakong.model;

import lombok.*;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Payment {
    private Integer id;
    private Integer orderId;
    private Float amount;
    private String paymentMethod;
    private String paymentStatus;
    private String md5;
    private String qrCode;
    private Date createdAt;
    private Date updatedAt;
}
