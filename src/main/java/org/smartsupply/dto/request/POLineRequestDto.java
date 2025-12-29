package org.smartsupply.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineRequestDto {
    @NotNull
    private Long productId;
    @Min(1)
    private Integer qty;
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;
}
