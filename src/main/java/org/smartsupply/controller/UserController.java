package org.smartsupply.controller;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.smartsupply.annotation.RequireAuth;
import org.smartsupply.annotation.RequireRole;
import org.smartsupply.dto.request.UserUpdateDto;
import org.smartsupply.dto.response.UserStatsDto;
import org.smartsupply.mapper.UserMapper;
import org.smartsupply.dto.response.UserResponseDto;
import org.smartsupply.model.entity.User;
import org.smartsupply.model.enums.Role;
import org.smartsupply.service.UserService;
import org.smartsupply.service.implementation.UserContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserContext userContext;
    private final UserMapper userMapper;
    private final UserService userService;

    @GetMapping("/me")
    @RequireAuth
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }

    @GetMapping("/all")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/active")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<List<UserResponseDto>> getActiveUsers() {
        List<UserResponseDto> users = userService.getActiveUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/inactive")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<List<UserResponseDto>> getInactiveUsers() {
        List<UserResponseDto> users = userService.getInactiveUsers();
        return ResponseEntity.ok(users);
    }


    @GetMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<UserResponseDto> getUserById(@PathVariable Long id) {
        UserResponseDto user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }


    @GetMapping("/role/{role}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<List<UserResponseDto>> getUsersByRole(@PathVariable Role role) {
        List<UserResponseDto> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<UserResponseDto> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateDto userUpdateDto) {
        UserResponseDto updatedUser = userService.updateUser(id, userUpdateDto);
        return ResponseEntity.ok(updatedUser);
    }


    @PatchMapping("/{id}/activate")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<UserResponseDto> activateUser(@PathVariable Long id) {
        UserResponseDto user = userService.activateUser(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}/deactivate")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<UserResponseDto> deactivateUser(@PathVariable Long id) {
        UserResponseDto user = userService.deactivateUser(id);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/search")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<List<UserResponseDto>> searchUsers(@RequestParam String keyword) {
        List<UserResponseDto> users = userService.searchUsers(keyword);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/stats")
    @RequireRole(Role.ADMIN)
    public ResponseEntity<UserStatsDto> getUserStats() {
        UserStatsDto stats = userService.getUserStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/warehouse")
    @RequireRole({Role.ADMIN, Role.WAREHOUSE_MANAGER})
    public ResponseEntity<UserResponseDto> warehouseAccess() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }


    @GetMapping("/client-area")
    @RequireRole(Role.CLIENT)
    public ResponseEntity<UserResponseDto> clientArea() {
        User user = userContext.getCurrentUser();
        return ResponseEntity.ok(userMapper.toResponseDto(user));
    }
}
