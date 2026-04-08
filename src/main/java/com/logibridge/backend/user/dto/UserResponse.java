package com.logibridge.backend.user.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;

    private String firstName;
    private String lastName;

    private String email;
    private String phoneNumber;

    private List<String> roles;

    private boolean enabled;
    private boolean accountNonLocked;
}