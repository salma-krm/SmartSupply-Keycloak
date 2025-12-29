package org.smartsupply.service;

import org.smartsupply.dto.request.UserUpdateDto;
import org.smartsupply.dto.response.UserResponseDto;
import org.smartsupply.dto.response.UserStatsDto;
import org.smartsupply.model.enums.Role;

import java.util.List;

public interface UserService {


    List<UserResponseDto> getAllUsers();

    List<UserResponseDto> getActiveUsers();


    List<UserResponseDto> getInactiveUsers();


    UserResponseDto getUserById(Long id);

    UserResponseDto getUserByEmail(String email);

    List<UserResponseDto> getUsersByRole(Role role);


    UserResponseDto updateUser(Long id, UserUpdateDto userUpdateDto);


    UserResponseDto activateUser(Long id);


    UserResponseDto deactivateUser(Long id);


    void deleteUser(Long id);


    List<UserResponseDto> searchUsers(String keyword);


    boolean existsById(Long id);


    UserStatsDto getUserStats();
}