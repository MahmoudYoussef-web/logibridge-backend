package com.logibridge.backend.user.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phoneNumber;
}