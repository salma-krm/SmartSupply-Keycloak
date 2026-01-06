package org.smartsupply.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventorySummaryDto {
    private Long inventoryId;
    private Long productId;
    private String productSku;
    private Integer qtyOnHand;
    private Integer qtyReserved;
    private Integer available;
    private Long warehouseId;
    private String warehouseName;
}