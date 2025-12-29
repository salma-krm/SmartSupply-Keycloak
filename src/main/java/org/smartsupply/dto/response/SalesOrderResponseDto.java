package org.smartsupply.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderResponseDto {
    private Long id;
    private String status;
    private LocalDateTime createdAt;

    private Long clientId;
    private String clientName;

    private Long warehouseId;
    private String warehouseName;

    private List<SalesOrderLineResponseDto> lines;

    private List<String> warnings;
}