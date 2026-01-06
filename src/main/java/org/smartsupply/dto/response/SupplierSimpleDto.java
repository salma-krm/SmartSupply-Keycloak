package org.smartsupply.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierSimpleDto {
    private Long id;
    private String name;
}
