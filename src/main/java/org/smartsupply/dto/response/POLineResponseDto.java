package org.smartsupply.dto.response;
import lombok.*;
import java.math.BigDecimal;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class POLineResponseDto {
    private Long id;
    private Long productId;
    private String productSku;
    private Integer qty;
    private BigDecimal price;
}
