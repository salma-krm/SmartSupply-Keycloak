package org.smartsupply.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesOrderRequestDto {
    @NotNull
    private Long clientId;

    @NotNull
    private Long warehouseId;

    @Valid
    private List<SalesOrderLineRequestDto> lines;
}