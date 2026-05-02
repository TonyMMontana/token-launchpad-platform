package com.ssoservice.service.impl;

import com.ssoservice.dto.user.UserLoginRequestDto;
import com.ssoservice.dto.user.UserLoginResponseDto;
import com.ssoservice.dto.user.UserRegistrationRequestDto;
import com.ssoservice.dto.user.UserRegistrationResponseDto;
import com.ssoservice.exception.domain.RegistrationException;
import com.ssoservice.model.RoleName;
import com.ssoservice.model.User;
import com.ssoservice.repository.UserRepository;
import com.ssoservice.security.JwtService;
import com.ssoservice.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public UserRegistrationResponseDto register(UserRegistrationRequestDto requestDto) {
        validate(requestDto);

        User user = mapToUser(requestDto);
        User savedUser = userRepository.save(user);
        return new UserRegistrationResponseDto(savedUser.getId().toString(), savedUser.getEmail());
    }

    @Override
    public UserLoginResponseDto login(UserLoginRequestDto requestDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(requestDto.email(), requestDto.password())
        );
        String token = jwtService.generateToken(authentication);
        return new UserLoginResponseDto(token);
    }

    private void validate(UserRegistrationRequestDto requestDto) {
        if (userRepository.existsByEmail(requestDto.email())) {
            throw new RegistrationException(
                    String.format("The email %s is already in use. Please user another email.", requestDto.email())
            );
        }
        if(!requestDto.password().equals(requestDto.repeatPassword())) {
            throw new RegistrationException("Passwords do not match.");
        }
    }

    private User mapToUser(UserRegistrationRequestDto requestDto) {
        User user = new User();
        user.setEmail(requestDto.email());
        user.setPassword(passwordEncoder.encode(requestDto.password()));
        user.setRoles(List.of(RoleName.USER));
        return user;
    }
}
