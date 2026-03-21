package edu.bridalshop.backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationMs;

    // ── Generate access token ──────────────────────────────────────────
    public String generateAccessToken(CustomUserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getEmail())
                .claim("userId",   userDetails.getUserId())
                .claim("publicId", userDetails.getPublicId())
                .claim("role",     userDetails.getAuthorities()
                        .iterator().next().getAuthority())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    // ── Extract email (subject) from token ─────────────────────────────
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    // ── Validate token against UserDetails ────────────────────────────
    public boolean isTokenValid(String token, CustomUserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getEmail()) && !isTokenExpired(token);
    }

    // ── Internal helpers ───────────────────────────────────────────────
    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }
}