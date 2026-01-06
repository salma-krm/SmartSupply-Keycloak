package org.smartsupply.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsDto {
    private Long totalUsers;
    private Long activeUsers;
    private Long inactiveUsers;
    private Long adminCount;
    private Long warehouseManagerCount;
    private Long clientCount;
}