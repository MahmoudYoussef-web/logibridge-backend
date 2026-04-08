package com.logibridge.backend.auth.mapper;

import com.logibridge.backend.auth.dto.RegisterRequest;
import com.logibridge.backend.auth.entity.User;

public class AuthMapper {

    private AuthMapper() {}

    public static User toUser(RegisterRequest request, String encodedPassword) {
        return User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(encodedPassword)
                .phoneNumber(request.getPhoneNumber())
                .build();
    }
}