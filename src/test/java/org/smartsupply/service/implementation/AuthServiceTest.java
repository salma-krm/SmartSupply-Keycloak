package org.smartsupply.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.smartsupply.dto.request.LoginRequestDto;
import org.smartsupply.dto.request.RegisterRequestDto;
import org.smartsupply.dto.response.AuthResponseDto;
import org.smartsupply.model.entity.User;
import org.smartsupply.repository.UserRepository;
import org.smartsupply.mapper.UserMapper;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour AuthService.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private SessionManager sessionManager;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {

    }

    private static String encodeSha256Base64(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void register_whenEmailAlreadyExists_throws() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.getEmail()).thenReturn("exists@example.com");
        when(userRepository.existsByEmail("exists@example.com")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.register(req));
        assertTrue(ex.getMessage().contains("Cet email est déjà utilisé"));
    }

    @Test
    void register_success_savesUserAndCreatesSession() {
        RegisterRequestDto req = mock(RegisterRequestDto.class);
        when(req.getEmail()).thenReturn("new@example.com");
        when(req.getPassword()).thenReturn("mypassword");

        User userEntity = new User();
        userEntity.setEmail("new@example.com");


        when(userMapper.toEntity(req)).thenReturn(userEntity);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                ReflectionTestUtils.setField(u, "id", 42L);
            }
            return u;
        });
        when(sessionManager.createSession(any(User.class))).thenReturn("session-xyz");
        when(userMapper.toResponseDto(any(User.class))).thenReturn(null);

        AuthResponseDto resp = authService.register(req);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertEquals(encodeSha256Base64("mypassword"), saved.getPassword());
        verify(sessionManager).createSession(saved);
        assertEquals("session-xyz", resp.getSessionId());
    }

    @Test
    void login_whenEmailNotFound_throws() {
        LoginRequestDto login = mock(LoginRequestDto.class);
        when(login.getEmail()).thenReturn("noone@example.com");
        when(userRepository.findByEmail("noone@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertTrue(ex.getMessage().contains("Email ou mot de passe incorrect"));
    }

    @Test
    void login_whenAccountInactive_throws() {
        LoginRequestDto login = mock(LoginRequestDto.class);
        when(login.getEmail()).thenReturn("user@example.com");
       // when(login.getPassword()).thenReturn("pwd");

        User stored = new User();
        stored.setIsActive(false);
        stored.setPassword(encodeSha256Base64("pwd"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(stored));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertTrue(ex.getMessage().contains("Votre compte est désactivé"));
    }

    @Test
    void login_whenWrongPassword_throws() {
        LoginRequestDto login = mock(LoginRequestDto.class);
        when(login.getEmail()).thenReturn("user@example.com");
        when(login.getPassword()).thenReturn("wrongpwd");

        User stored = new User();
        stored.setIsActive(true);
        stored.setPassword(encodeSha256Base64("correctpwd"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(stored));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(login));
        assertTrue(ex.getMessage().contains("Email ou mot de passe incorrect"));
    }

    @Test
    void login_success_createsSession_and_returnsResponse() {
        LoginRequestDto login = mock(LoginRequestDto.class);
        when(login.getEmail()).thenReturn("user@example.com");
        when(login.getPassword()).thenReturn("secret123");

        User stored = new User();
        stored.setIsActive(true);
        stored.setPassword(encodeSha256Base64("secret123"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(stored));
        when(userMapper.toResponseDto(stored)).thenReturn(null);
        when(sessionManager.createSession(stored)).thenReturn("sess-321");

        AuthResponseDto resp = authService.login(login);

        assertEquals("sess-321", resp.getSessionId());
        verify(sessionManager).createSession(stored);
    }

    @Test
    void logout_delegatesToSessionManager() {
        authService.logout("sid-123");
        verify(sessionManager).invalidateSession("sid-123");
    }
}
