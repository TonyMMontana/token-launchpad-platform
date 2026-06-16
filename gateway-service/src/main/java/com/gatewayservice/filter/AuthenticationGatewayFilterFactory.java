package com.gatewayservice.filter;

import static com.launchpad.common.header.InternalHeaders.IDEMPOTENCY_KEY_HEADER;
import static com.launchpad.common.header.InternalHeaders.USER_ID_HEADER;
import static com.launchpad.common.header.InternalHeaders.USER_ROLES_HEADER;

import com.gatewayservice.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    public AuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            if (exchange.getRequest().getURI().getPath().contains("/auth")) {
                return chain.filter(exchange);
            }
            if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Missing Authorization Header");
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                String userId;
                List<String> roles;
                try {
                    jwtUtil.validateToken(token);
                    userId = jwtUtil.extractUser(token);
                    roles = jwtUtil.extractRoles(token);
                } catch (Exception e) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
                }

                if (roles == null || roles.isEmpty()) {
                    return onError(exchange, HttpStatus.UNAUTHORIZED, "No roles found");
                }
                String rolesHeader = String.join(",", roles);

                String idempotencyKey = exchange.getRequest().getHeaders().getFirst(IDEMPOTENCY_KEY_HEADER);

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .headers(headers -> {
                            headers.remove(USER_ID_HEADER);
                            headers.remove(USER_ROLES_HEADER);

                            headers.set(USER_ID_HEADER, userId);
                            headers.set(USER_ROLES_HEADER, rolesHeader);

                            if (idempotencyKey != null) {
                                headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
                            }
                        })
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } else {
                return onError(exchange, HttpStatus.UNAUTHORIZED, "Invalid Authorization Header format");
            }
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status, String errorMessage) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonBody = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\"}",
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                errorMessage
        );

        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}