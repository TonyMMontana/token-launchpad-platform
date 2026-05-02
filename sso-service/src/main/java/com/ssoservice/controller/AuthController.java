package com.ssoservice.controller;

import com.ssoservice.dto.user.UserLoginRequestDto;
import com.ssoservice.dto.user.UserLoginResponseDto;
import com.ssoservice.dto.user.UserRegistrationRequestDto;
import com.ssoservice.dto.user.UserRegistrationResponseDto;
import com.ssoservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponseDto> register(@RequestBody UserRegistrationRequestDto userRegistrationRequestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.register(userRegistrationRequestDto));
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(@RequestBody UserLoginRequestDto userLoginRequestDto) {
        return ResponseEntity.status(HttpStatus.OK).body(authenticationService.login(userLoginRequestDto));
    }
}
