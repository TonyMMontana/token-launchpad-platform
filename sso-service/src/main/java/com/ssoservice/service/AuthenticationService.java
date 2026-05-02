package com.ssoservice.service;


import com.ssoservice.dto.user.UserLoginRequestDto;
import com.ssoservice.dto.user.UserLoginResponseDto;
import com.ssoservice.dto.user.UserRegistrationRequestDto;
import com.ssoservice.dto.user.UserRegistrationResponseDto;

public interface AuthenticationService {
    UserRegistrationResponseDto register(UserRegistrationRequestDto requestDto);
    UserLoginResponseDto login(UserLoginRequestDto userRequestDto);
}
