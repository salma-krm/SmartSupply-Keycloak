package org.smartsupply.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import org.smartsupply.dto.request.RegisterRequestDto;
import org.smartsupply.dto.response.UserResponseDto;
import org.smartsupply.model.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    @Mapping(target = "password", ignore = true)
    User toEntity(RegisterRequestDto registerRequestDto);

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    User toEntity(UserResponseDto dto);

    UserResponseDto toResponseDto(User user);
}