package org.smartsupply.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderLineResponseDto {
    private Long id;
    private Long productId;
    private String productSku;
    private String productName;
    private int qtyOrdered;
    private int qtyReserved;
    private BigDecimal price;
}