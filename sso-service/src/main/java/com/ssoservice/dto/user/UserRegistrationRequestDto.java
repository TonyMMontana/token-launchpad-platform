package com.ssoservice.dto.user;

public record UserRegistrationRequestDto(
        String email,
        String password,
        String repeatPassword
) {
}
