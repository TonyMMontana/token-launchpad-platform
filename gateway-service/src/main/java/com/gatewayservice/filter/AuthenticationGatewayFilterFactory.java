package com.gatewayservice.filter;

import com.gatewayservice.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthenticationGatewayFilterFactory.Config> {

    public static final String USER_ID_HEADER = "X-User-Id";
    private static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

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
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                jwtUtil.validateToken(token);

                String userId = jwtUtil.extractUser(token);
                String idempotencyKey = exchange.getRequest().getHeaders().getFirst(IDEMPOTENCY_KEY_HEADER);

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .headers(headers -> {
                            headers.set(USER_ID_HEADER, userId);

                            if (idempotencyKey != null) {
                                headers.set(IDEMPOTENCY_KEY_HEADER, idempotencyKey);
                            }
                        })
                        .build();

                return chain.filter(exchange.mutate().request(modifiedRequest).build());
            } else {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        });
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }
}