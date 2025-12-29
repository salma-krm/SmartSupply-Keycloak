package org.smartsupply.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import org.smartsupply.model.enums.POStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponseDto {
    private Long id;
    private Long supplierId;
    private String supplierName;
    private POStatus status;
    private LocalDateTime createdAt;
    private List<POLineResponseDto> lines;
}
