package org.smartsupply.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDetailResponseDto {
    private Long id;
    private String name;
    private Integer productCount;
    private List<ProductSummaryDto> products;
}