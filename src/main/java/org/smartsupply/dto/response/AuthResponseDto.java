package org.smartsupply.dto.response;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

    private String message;
    private UserResponseDto user;
    private String sessionId;
}