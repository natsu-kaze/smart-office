package com.natsukaze.smartoffice.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String REAL_NAME_HEADER = "X-Real-Name";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/actuator/health",
            "/actuator/info"
    );

    private final GatewayJwtService jwtService;

    public GatewayAuthFilter(GatewayJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (isPublicPath(path)) {
            return chain.filter(stripIdentityHeaders(exchange));
        }

        String token = resolveToken(exchange.getRequest());
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "missing token");
        }

        try {
            Claims claims = jwtService.parseAndValidate(token);
            Object userId = claims.get("userId");
            if (userId == null) {
                return unauthorized(exchange, "invalid token");
            }
            ServerHttpRequest request = exchange.getRequest().mutate()
                    .headers(headers -> {
                        headers.remove(USER_ID_HEADER);
                        headers.remove(USERNAME_HEADER);
                        headers.remove(REAL_NAME_HEADER);
                    })
                    .header(USER_ID_HEADER, String.valueOf(userId))
                    .header(USERNAME_HEADER, claims.getSubject())
                    .header(REAL_NAME_HEADER, String.valueOf(claims.get("realName", String.class)))
                    .build();
            return chain.filter(exchange.mutate().request(request).build());
        } catch (RuntimeException ex) {
            return unauthorized(exchange, "invalid token");
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USERNAME_HEADER);
                    headers.remove(REAL_NAME_HEADER);
                })
                .build();
        return exchange.mutate().request(request).build();
    }

    private String resolveToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authorization) && authorization.startsWith(BEARER_PREFIX)) {
            return authorization.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"code\":401,\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }
}
