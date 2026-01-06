package org.smartsupply.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.smartsupply.dto.request.LoginRequestDto;
import org.smartsupply.dto.request.RegisterRequestDto;
import org.smartsupply.dto.response.AuthResponseDto;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.implementation.AuthService;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;
    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void register_shouldReturnCreatedAndBody() throws Exception {
        RegisterRequestDto req = new RegisterRequestDto();
        req.setEmail("u@example.com");
        req.setPassword("password123"); // >=8 caractères
        req.setFirstName("John");
        req.setLastName("Doe");
        req.setRole(Role.CLIENT); // Enum ou String selon ton DTO

        AuthResponseDto resp = AuthResponseDto.builder()
                .message("Inscription réussie")
                .sessionId("s1")
                .build();
        when(authService.register(any(RegisterRequestDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Inscription réussie"))
                .andExpect(jsonPath("$.sessionId").value("s1"));

        verify(authService).register(any(RegisterRequestDto.class));
    }

    @Test
    void login_shouldReturnOkAndBody() throws Exception {
        LoginRequestDto req = new LoginRequestDto();
        req.setEmail("u@example.com");
        req.setPassword("pwd");

        AuthResponseDto resp = AuthResponseDto.builder().message("Connexion réussie").sessionId("sess").build();
        when(authService.login(any(LoginRequestDto.class))).thenReturn(resp);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Connexion réussie"))
                .andExpect(jsonPath("$.sessionId").value("sess"));

        verify(authService).login(any(LoginRequestDto.class));
    }

    @Test
    void logout_shouldInvokeServiceAndReturnMessage() throws Exception {
        doNothing().when(authService).logout("sid-1");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Session-Id", "sid-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Déconnexion réussie"));

        verify(authService).logout("sid-1");
    }
}