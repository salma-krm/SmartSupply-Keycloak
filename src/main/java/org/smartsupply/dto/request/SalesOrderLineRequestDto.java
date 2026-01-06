package org.smartsupply.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderLineRequestDto {
    @NotNull
    private Long productId;

    @Min(1)
    private int qtyOrdered;

}