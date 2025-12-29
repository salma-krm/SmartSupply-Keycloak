package org.smartsupply.dto.response;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseDetailDto {
    private Long id;
    private String code;
    private String name;
    private Boolean active;
    // hena kayjib m3ah 7ta inventorySummary bela may3awed i referencier 3la warehouse
    private List<InventorySummaryDto> inventories;
}