package com.natsukaze.smartoffice.gateway.security;

import io.jsonwebtoken.Claims;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class GatewayAuthFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    private static final String REAL_NAME_HEADER = "X-Real-Name";
    private static final String ROLES_HEADER = "X-User-Roles";
    private static final String PERMISSIONS_HEADER = "X-User-Permissions";
    private static final String ADMIN_ROLE = "ADMIN";
    private static final String ADMIN_USERNAME = "admin";
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/actuator/health",
            "/actuator/info"
    );
    private static final Map<String, String> READ_PERMISSIONS = Map.ofEntries(
            Map.entry("/api/system/users", "sys:user:list"),
            Map.entry("/api/system/roles", "sys:role:list"),
            Map.entry("/api/system/menus", "sys:role:list"),
            Map.entry("/api/org", "org:manage"),
            Map.entry("/api/approvals", "approval:list"),
            Map.entry("/api/messages", "message:list"),
            Map.entry("/api/files", "file:list"),
            Map.entry("/api/policies", "policy:list"),
            Map.entry("/api/attendance", "attendance:list")
    );

    private final GatewayJwtService jwtService;

    private final GatewayTokenStore tokenStore;

    public GatewayAuthFilter(GatewayJwtService jwtService, GatewayTokenStore tokenStore) {
        this.jwtService = jwtService;
        this.tokenStore = tokenStore;
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
            List<String> roles = new ArrayList<>(claimList(claims, "roles"));
            List<String> permissions = claimList(claims, "permissions");
            if (isAdminSubject(claims) && roles.stream().noneMatch(ADMIN_ROLE::equalsIgnoreCase)) {
                roles.add(ADMIN_ROLE);
            }
            String requiredPermission = requiredPermission(path, exchange.getRequest().getMethod());
            boolean admin = roles.stream().anyMatch(ADMIN_ROLE::equalsIgnoreCase);
            if (requiredPermission != null && !admin && !permissions.contains(requiredPermission)) {
                return forbidden(exchange, "permission denied");
            }
            return tokenStore.isActive(token)
                    .flatMap(active -> {
                        if (!active) {
                            return unauthorized(exchange, "invalid token");
                        }
                        ServerHttpRequest request = exchange.getRequest().mutate()
                                .headers(headers -> {
                                    headers.remove(USER_ID_HEADER);
                                    headers.remove(USERNAME_HEADER);
                                    headers.remove(REAL_NAME_HEADER);
                                    headers.remove(ROLES_HEADER);
                                    headers.remove(PERMISSIONS_HEADER);
                                })
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(USERNAME_HEADER, claims.getSubject())
                                .header(REAL_NAME_HEADER, String.valueOf(claims.get("realName", String.class)))
                                .header(ROLES_HEADER, String.join(",", roles))
                                .header(PERMISSIONS_HEADER, String.join(",", permissions))
                                .build();
                        return chain.filter(exchange.mutate().request(request).build());
                    });
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
                    headers.remove(ROLES_HEADER);
                    headers.remove(PERMISSIONS_HEADER);
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
        return writeError(exchange, 401, message);
    }

    private Mono<Void> forbidden(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return writeError(exchange, 403, message);
    }

    private Mono<Void> writeError(ServerWebExchange exchange, int code, String message) {
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"code\":" + code + ",\"message\":\"" + message + "\",\"data\":null}")
                .getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse()
                .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    private String requiredPermission(String path, HttpMethod method) {
        if (path.startsWith("/api/auth")) {
            return null;
        }
        if (path.startsWith("/api/system/users/options")) {
            return "org:manage";
        }
        if (path.startsWith("/api/system/users/profile")) {
            return null;
        }
        if (path.startsWith("/api/system/users/") && path.endsWith("/roles")) {
            return "sys:user:role";
        }
        if (path.startsWith("/api/system/roles/") && path.endsWith("/menus")) {
            return "sys:role:menu";
        }
        if (path.startsWith("/api/system/roles") && !HttpMethod.GET.equals(method)) {
            return "sys:role:save";
        }
        if (path.startsWith("/api/messages/announcements")) {
            return "message:announcement:send";
        }
        if (path.startsWith("/api/approvals/rules") && !HttpMethod.GET.equals(method)) {
            return "approval:rule:manage";
        }
        if (path.startsWith("/api/files") && HttpMethod.DELETE.equals(method)) {
            return "file:list";
        }
        if (path.startsWith("/api/policies") && !HttpMethod.GET.equals(method)) {
            return "policy:manage";
        }
        if (path.startsWith("/api/attendance/department-records")) {
            return "attendance:department:list";
        }
        return READ_PERMISSIONS.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private List<String> claimList(Claims claims, String name) {
        Object value = claims.get(name);
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return List.of();
    }

    private boolean isAdminSubject(Claims claims) {
        return ADMIN_USERNAME.equalsIgnoreCase(claims.getSubject());
    }
}
