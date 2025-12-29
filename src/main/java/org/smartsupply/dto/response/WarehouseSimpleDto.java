package org.smartsupply.dto.response;

import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseSimpleDto {
    private Long id;
    private String code;
    private String name;
    private Boolean active;
}
