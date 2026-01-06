package org.smartsupply.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequestDto {
    @NotNull
    private Long productId;
    @NotNull
    private Long warehouseId;
    @Min(value = 1, message = "La quantité doit être >= 1")
    private Integer qty;
    private String reference;
}