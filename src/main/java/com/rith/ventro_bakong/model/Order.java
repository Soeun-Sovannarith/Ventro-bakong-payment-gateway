package com.rith.ventro_bakong.model;

import lombok.*;
import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Order {
    private Integer id;
    private Float totalAmount;
    private String status;
    private Date createdAt;
}
