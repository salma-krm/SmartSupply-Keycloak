package org.smartsupply.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PurchaseOrderRequestDto {
    @NotNull
    private Long supplierId;

    @Size(min = 1)
    private List<POLineRequestDto> lines;


    private String reference;
}
