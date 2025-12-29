package org.smartsupply.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImpTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImp service;

    @BeforeEach
    void setUp() {

        lenient().when(userMapper.toResponseDto(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            UserResponseDto dto = new UserResponseDto();
            dto.setId(u.getId());
            dto.setEmail(u.getEmail());
            dto.setFirstName(u.getFirstName());
            dto.setLastName(u.getLastName());
            dto.setRole(u.getRole());
            dto.setIsActive(u.getIsActive());
            return dto;
        });
    }

    private User makeUser(Long id, String email, boolean active, Role role) {
        User u = new User();
        ReflectionTestUtils.setField(u, "id", id);
        u.setEmail(email);
        u.setFirstName("FN" + id);
        u.setLastName("LN" + id);
        u.setIsActive(active);
        u.setRole(role);
        return u;
    }

    @Test
    void getAllUsers_returnsMappedList() {
        User u1 = makeUser(1L, "a@example.com", true, Role.CLIENT);
        User u2 = makeUser(2L, "b@example.com", false, Role.ADMIN);
        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        List<UserResponseDto> res = service.getAllUsers();

        assertEquals(2, res.size());
        assertEquals(1L, res.get(0).getId());
        assertEquals("a@example.com", res.get(0).getEmail());
        verify(userRepository).findAll();
        verify(userMapper, times(2)).toResponseDto(any(User.class));
    }

    @Test
    void getActiveUsers_returnsOnlyActive() {
        User u = makeUser(3L, "act@example.com", true, Role.WAREHOUSE_MANAGER);
        when(userRepository.findByIsActiveTrue()).thenReturn(Collections.singletonList(u));

        List<UserResponseDto> res = service.getActiveUsers();

        assertEquals(1, res.size());
        assertTrue(res.get(0).getIsActive());
        verify(userRepository).findByIsActiveTrue();
    }

    @Test
    void getInactiveUsers_returnsOnlyInactive() {
        User u = makeUser(4L, "inact@example.com", false, Role.CLIENT);
        when(userRepository.findByIsActiveFalse()).thenReturn(Collections.singletonList(u));

        List<UserResponseDto> res = service.getInactiveUsers();

        assertEquals(1, res.size());
        assertFalse(res.get(0).getIsActive());
        verify(userRepository).findByIsActiveFalse();
    }

    @Test
    void getUserById_found_returnsDto() {
        User u = makeUser(5L, "u5@example.com", true, Role.CLIENT);
        when(userRepository.findById(5L)).thenReturn(Optional.of(u));

        UserResponseDto dto = service.getUserById(5L);

        assertEquals(5L, dto.getId());
        assertEquals("u5@example.com", dto.getEmail());
        verify(userRepository).findById(5L);
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getUserById(99L));
        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void getUserByEmail_found_returnsDto() {
        User u = makeUser(6L, "byemail@example.com", true, Role.ADMIN);
        when(userRepository.findByEmail("byemail@example.com")).thenReturn(Optional.of(u));

        UserResponseDto dto = service.getUserByEmail("byemail@example.com");

        assertEquals(6L, dto.getId());
        assertEquals("byemail@example.com", dto.getEmail());
        verify(userRepository).findByEmail("byemail@example.com");
    }

    @Test
    void getUserByEmail_notFound_throws() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.getUserByEmail("missing@example.com"));
        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void getUsersByRole_returnsMapped() {
        User u1 = makeUser(7L, "r1@example.com", true, Role.ADMIN);
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(Collections.singletonList(u1));

        List<UserResponseDto> res = service.getUsersByRole(Role.ADMIN);

        assertEquals(1, res.size());
        assertEquals(Role.ADMIN, res.get(0).getRole());
        verify(userRepository).findByRole(Role.ADMIN);
    }

    @Test
    void updateUser_success_updatesAndReturnsDto() {
        User existing = makeUser(10L, "old@example.com", true, Role.CLIENT);
        when(userRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("new@example.com", 10L)).thenReturn(false);

        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("new@example.com");
        dto.setFirstName("NewFN");
        dto.setLastName("NewLN");
        dto.setRole(Role.WAREHOUSE_MANAGER);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto res = service.updateUser(10L, dto);

        assertEquals(10L, res.getId());
        verify(userRepository).save(existing);
        assertEquals("new@example.com", existing.getEmail());
        assertEquals("NewFN", existing.getFirstName());
        assertEquals(Role.WAREHOUSE_MANAGER, existing.getRole());
    }

    @Test
    void updateUser_duplicateEmail_throwsDuplicate() {
        User existing = makeUser(11L, "old2@example.com", true, Role.CLIENT);
        when(userRepository.findById(11L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmailAndIdNot("dup@example.com", 11L)).thenReturn(true);

        UserUpdateDto dto = new UserUpdateDto();
        dto.setEmail("dup@example.com");
        dto.setFirstName("X");
        dto.setLastName("Y");
        dto.setRole(Role.CLIENT);

        DuplicateResourceException ex = assertThrows(DuplicateResourceException.class, () -> service.updateUser(11L, dto));
        assertTrue(ex.getMessage().contains("Cet email"));
    }

    @Test
    void updateUser_notFound_throws() {
        when(userRepository.findById(222L)).thenReturn(Optional.empty());
        UserUpdateDto dto = new UserUpdateDto();
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.updateUser(222L, dto));
        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void activateUser_success_activates() {
        User u = makeUser(20L, "toactivate@example.com", false, Role.CLIENT);
        when(userRepository.findById(20L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto res = service.activateUser(20L);

        assertTrue(u.getIsActive());
        assertEquals(20L, res.getId());
        verify(userRepository).save(u);
    }

    @Test
    void activateUser_alreadyActive_throwsBusiness() {
        User u = makeUser(21L, "act@example.com", true, Role.CLIENT);
        when(userRepository.findById(21L)).thenReturn(Optional.of(u));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.activateUser(21L));
        assertTrue(ex.getMessage().contains("déjà actif"));
    }

    @Test
    void activateUser_notFound_throws() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.activateUser(999L));
        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void deactivateUser_success_deactivates() {
        User u = makeUser(30L, "todeact@example.com", true, Role.CLIENT);
        when(userRepository.findById(30L)).thenReturn(Optional.of(u));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDto res = service.deactivateUser(30L);

        assertFalse(u.getIsActive());
        assertEquals(30L, res.getId());
        verify(userRepository).save(u);
    }

    @Test
    void deactivateUser_alreadyInactive_throwsBusiness() {
        User u = makeUser(31L, "already@example.com", false, Role.CLIENT);
        when(userRepository.findById(31L)).thenReturn(Optional.of(u));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deactivateUser(31L));
        assertTrue(ex.getMessage().contains("déjà inactif"));
    }

    @Test
    void deactivateUser_notFound_throws() {
        when(userRepository.findById(1234L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deactivateUser(1234L));
        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void deleteUser_success_deletes() {
        User u = makeUser(40L, "del@example.com", true, Role.CLIENT);
        when(userRepository.findById(40L)).thenReturn(Optional.of(u));

        service.deleteUser(40L);

        verify(userRepository).delete(u);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.findById(404L)).thenReturn(Optional.empty());
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteUser(404L));
        assertTrue(ex.getMessage().contains("Utilisateur introuvable"));
    }

    @Test
    void searchUsers_delegatesToRepo_andMaps() {
        User u = makeUser(50L, "s1@example.com", true, Role.CLIENT);
        when(userRepository.searchUsers("key")).thenReturn(Collections.singletonList(u));

        List<UserResponseDto> res = service.searchUsers("key");

        assertEquals(1, res.size());
        assertEquals(50L, res.get(0).getId());
        verify(userRepository).searchUsers("key");
        verify(userMapper).toResponseDto(u);
    }

    @Test
    void existsById_delegates() {
        when(userRepository.existsById(77L)).thenReturn(true);
        assertTrue(service.existsById(77L));
        verify(userRepository).existsById(77L);
    }

    @Test
    void getUserStats_returnsCorrectAggregation() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByIsActiveTrue()).thenReturn(7L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(2L);
        when(userRepository.countByRole(Role.WAREHOUSE_MANAGER)).thenReturn(3L);
        when(userRepository.countByRole(Role.CLIENT)).thenReturn(5L);

        UserStatsDto stats = service.getUserStats();

        assertEquals(10L, stats.getTotalUsers());
        assertEquals(7L, stats.getActiveUsers());
        assertEquals(3L, stats.getInactiveUsers()); // 10 - 7
        assertEquals(2L, stats.getAdminCount());
        assertEquals(3L, stats.getWarehouseManagerCount());
        assertEquals(5L, stats.getClientCount());
        verify(userRepository).count();
        verify(userRepository).countByIsActiveTrue();
        verify(userRepository, times(3)).countByRole(any(Role.class));
    }
}