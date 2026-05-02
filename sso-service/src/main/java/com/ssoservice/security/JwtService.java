package com.ssoservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

@Service
public class JwtService {
    @Value("${application.security.jwt.secret-key}")
    private String secret;
    @Value("${application.security.jwt.expiration}")
    private Long expiration;

    public String generateToken(Authentication authentication) {
        return Jwts.builder()
                .subject(authentication.getName())
                .claim("roles", getRoles(authentication))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    private SecretKey getSigningKey() {
        byte[] decoded = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(decoded);
    }

    private List<String> getRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }
}
