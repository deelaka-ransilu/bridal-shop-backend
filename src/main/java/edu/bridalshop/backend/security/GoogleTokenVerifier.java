package edu.bridalshop.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class GoogleTokenVerifier {

    @Value("${google.client-id}")
    private String clientId;

    @Getter
    public static class GoogleUserInfo {
        public final String googleId;
        public final String email;
        public final String fullName;
        public final String pictureUrl;

        public GoogleUserInfo(String googleId, String email,
                              String fullName, String pictureUrl) {
            this.googleId   = googleId;
            this.email      = email;
            this.fullName   = fullName;
            this.pictureUrl = pictureUrl;
        }
    }

    // ── Verify id_token (used for credential response) ─────────────────
    public GoogleUserInfo verifyIdToken(String idToken) {
        log.debug("Verifying Google ID token");
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier
                    .Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken == null) {
                log.warn("Invalid Google ID token: verification returned null");
                throw new RuntimeException("Invalid Google ID token");
            }

            GoogleIdToken.Payload payload = googleIdToken.getPayload();
            String email = payload.getEmail();
            log.info("Google ID token verified successfully for email: {}", email);
            return new GoogleUserInfo(
                    payload.getSubject(),
                    email,
                    (String) payload.get("name"),
                    (String) payload.get("picture")
            );
        } catch (Exception e) {
            log.error("Google ID token verification failed", e);
            throw new RuntimeException("Google token verification failed: "
                    + e.getMessage(), e);
        }
    }

    // ── Verify access_token via Google userinfo endpoint ───────────────
    public GoogleUserInfo verifyAccessToken(String accessToken) {
        log.debug("Verifying Google access token");
        try {
            RestTemplate restTemplate = new RestTemplate();

            org.springframework.http.HttpHeaders headers =
                    new org.springframework.http.HttpHeaders();
            headers.setBearerAuth(accessToken);

            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            org.springframework.http.ResponseEntity<Map> response =
                    restTemplate.exchange(
                            "https://www.googleapis.com/oauth2/v3/userinfo",
                            org.springframework.http.HttpMethod.GET,
                            entity,
                            Map.class
                    );

            @SuppressWarnings("unchecked")
            Map<String, Object> userInfo = response.getBody();

            if (userInfo == null || userInfo.get("sub") == null) {
                log.warn("Invalid Google access token: userInfo missing or sub null");
                throw new RuntimeException("Invalid Google access token");
            }

            String email = (String) userInfo.get("email");
            log.info("Google access token verified successfully for email: {}", email);
            return new GoogleUserInfo(
                    (String) userInfo.get("sub"),
                    email,
                    (String) userInfo.get("name"),
                    (String) userInfo.get("picture")
            );
        } catch (Exception e) {
            log.error("Google access token verification failed", e);
            throw new RuntimeException(
                    "Google token verification failed: " + e.getMessage(), e);
        }
    }

    // ── Always use access token verification ──────────────────────────
    public GoogleUserInfo verify(String token) {
        log.debug("Verifying Google token (access token method)");
        return verifyAccessToken(token);
    }
}