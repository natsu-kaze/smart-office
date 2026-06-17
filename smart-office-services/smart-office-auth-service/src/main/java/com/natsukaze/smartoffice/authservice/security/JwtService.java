package com.natsukaze.smartoffice.authservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.getExpiration());
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(principal.getUsername())
                .claim("userId", principal.getUserId())
                .claim("realName", principal.getRealName())
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(signingKey)
                .compact();
    }

    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserPrincipal principal) {
        Claims claims = getClaims(token);
        return principal.getUsername().equals(claims.getSubject())
                && claims.getExpiration().after(new Date());
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
