package org.smartsupply.service.implementation;
import lombok.RequiredArgsConstructor;
import org.smartsupply.mapper.UserMapper;
import org.smartsupply.dto.request.LoginRequestDto;
import org.smartsupply.dto.request.RegisterRequestDto;
import org.smartsupply.dto.response.AuthResponseDto;
import org.smartsupply.model.entity.User;
import org.smartsupply.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SessionManager sessionManager;

    @Transactional
    public AuthResponseDto register(RegisterRequestDto registerRequestDto) {
        if (userRepository.existsByEmail(registerRequestDto.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = userMapper.toEntity(registerRequestDto);
        user.setPassword(encodePassword(registerRequestDto.getPassword()));

        User savedUser = userRepository.save(user);
        String sessionId = sessionManager.createSession(savedUser);

        return AuthResponseDto.builder()
                .message("Inscription réussie")
                .user(userMapper.toResponseDto(savedUser))
                .sessionId(sessionId)
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        if (!user.getIsActive()) {
            throw new RuntimeException("Votre compte est désactivé");
        }

        if (!checkPassword(loginRequestDto.getPassword(), user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        String sessionId = sessionManager.createSession(user);

        return AuthResponseDto.builder()
                .message("Connexion réussie")
                .user(userMapper.toResponseDto(user))
                .sessionId(sessionId)
                .build();
    }

    public void logout(String sessionId) {
        sessionManager.invalidateSession(sessionId);
    }

    private String encodePassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Erreur lors de l'encodage du mot de passe", e);
        }
    }

    private boolean checkPassword(String rawPassword, String encodedPassword) {
        return encodePassword(rawPassword).equals(encodedPassword);
    }
}