package org.smartsupply.dto.response;


import lombok.*;
import org.smartsupply.model.enums.Role;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private Boolean isActive;
}