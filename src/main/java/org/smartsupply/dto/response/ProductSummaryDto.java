package org.smartsupply.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSummaryDto {
    private Long id;
    private String sku;
    private String name;
    private BigDecimal originalPrice;
    private BigDecimal profit;
    private BigDecimal sellingPrice;
    private String unit;
    private Boolean active;
}