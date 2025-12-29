package org.smartsupply.service.implementation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.smartsupply.dto.request.UserUpdateDto;
import org.smartsupply.dto.response.UserResponseDto;
import org.smartsupply.dto.response.UserStatsDto;
import org.smartsupply.exception.BusinessException;
import org.smartsupply.exception.DuplicateResourceException;
import org.smartsupply.exception.ResourceNotFoundException;
import org.smartsupply.mapper.UserMapper;
import org.smartsupply.model.entity.User;
import org.smartsupply.model.enums.Role;
import org.smartsupply.repository.UserRepository;
import org.smartsupply.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserServiceImp implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public List<UserResponseDto> getAllUsers() {
        log.info("Récupération de tous les utilisateurs");
        List<User> users = userRepository.findAll();
        log.info("{} utilisateur(s) trouvé(s)", users.size());
        return users.stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    public List<UserResponseDto> getActiveUsers() {
        log.info("Récupération des utilisateurs actifs");
        List<User> users = userRepository.findByIsActiveTrue();
        log.info("{} utilisateur(s) actif(s) trouvé(s)", users.size());
        return users.stream()
                .map(userMapper::toResponseDto)
                .toList();
    }


    @Override
    public List<UserResponseDto> getInactiveUsers() {
        log.info("Récupération des utilisateurs inactifs");
        List<User> users = userRepository.findByIsActiveFalse();
        log.info("{} utilisateur(s) inactif(s) trouvé(s)", users.size());
        return users.stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    public UserResponseDto getUserById(Long id) {
        log.info("Recherche de l'utilisateur avec l'ID: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID: " + id));
        return userMapper.toResponseDto(user);
    }

    @Override
    public UserResponseDto getUserByEmail(String email) {
        log.info("Recherche de l'utilisateur avec l'email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'email: " + email));
        return userMapper.toResponseDto(user);
    }

    @Override
    public List<UserResponseDto> getUsersByRole(Role role) {
        log.info("Récupération des utilisateurs avec le rôle: {}", role);
        List<User> users = userRepository.findByRole(role);
        log.info("{} utilisateur(s) trouvé(s) avec le rôle {}", users.size(), role);
        return users.stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional
    public UserResponseDto updateUser(Long id, UserUpdateDto userUpdateDto) {
        log.info("Mise à jour de l'utilisateur avec l'ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID: " + id));


        if (userRepository.existsByEmailAndIdNot(userUpdateDto.getEmail(), id)) {
            log.error("L'email '{}' est déjà utilisé par un autre utilisateur", userUpdateDto.getEmail());
            throw new DuplicateResourceException("Cet email est déjà utilisé par un autre utilisateur");
        }


        user.setFirstName(userUpdateDto.getFirstName());
        user.setLastName(userUpdateDto.getLastName());
        user.setEmail(userUpdateDto.getEmail());
        user.setRole(userUpdateDto.getRole());

        User updatedUser = userRepository.save(user);
        log.info("Utilisateur mis à jour avec succès. ID: {}", updatedUser.getId());

        return userMapper.toResponseDto(updatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto activateUser(Long id) {
        log.info("Activation de l'utilisateur avec l'ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID: " + id));

        if (user.getIsActive()) {
            log.warn("L'utilisateur {} est déjà actif", id);
            throw new BusinessException("Cet utilisateur est déjà actif");
        }

        user.setIsActive(true);
        User activatedUser = userRepository.save(user);
        log.info("Utilisateur activé avec succès. ID: {}", activatedUser.getId());

        return userMapper.toResponseDto(activatedUser);
    }

    @Override
    @Transactional
    public UserResponseDto deactivateUser(Long id) {
        log.info("Désactivation de l'utilisateur avec l'ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID: " + id));

        if (!user.getIsActive()) {
            log.warn("L'utilisateur {} est déjà inactif", id);
            throw new BusinessException("Cet utilisateur est déjà inactif");
        }

        user.setIsActive(false);
        User deactivatedUser = userRepository.save(user);
        log.info("Utilisateur désactivé avec succès. ID: {}", deactivatedUser.getId());

        return userMapper.toResponseDto(deactivatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        log.info("Suppression de l'utilisateur avec l'ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable avec l'ID: " + id));

        userRepository.delete(user);
        log.info("Utilisateur supprimé avec succès. ID: {}", id);
    }

    @Override
    public List<UserResponseDto> searchUsers(String keyword) {
        log.info("Recherche d'utilisateurs avec le mot-clé: {}", keyword);
        List<User> users = userRepository.searchUsers(keyword);
        log.info("{} utilisateur(s) trouvé(s)", users.size());
        return users.stream()
                .map(userMapper::toResponseDto)
                .toList();
    }

    @Override
    public boolean existsById(Long id) {
        return userRepository.existsById(id);
    }

    @Override
    public UserStatsDto getUserStats() {
        log.info("Récupération des statistiques utilisateurs");

        Long totalUsers = userRepository.count();
        Long activeUsers = userRepository.countByIsActiveTrue();
        Long inactiveUsers = totalUsers - activeUsers;
        Long adminCount = userRepository.countByRole(Role.ADMIN);
        Long warehouseManagerCount = userRepository.countByRole(Role.WAREHOUSE_MANAGER);
        Long clientCount = userRepository.countByRole(Role.CLIENT);

        return UserStatsDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .adminCount(adminCount)
                .warehouseManagerCount(warehouseManagerCount)
                .clientCount(clientCount)
                .build();
    }

}
