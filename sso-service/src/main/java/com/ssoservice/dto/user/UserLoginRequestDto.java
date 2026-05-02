package com.ssoservice.dto.user;

public record UserLoginRequestDto(
        String email,
        String password
) {
}
