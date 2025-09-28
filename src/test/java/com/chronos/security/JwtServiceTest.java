package com.chronos.security;

import com.chronos.domain.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        
        // Set test properties using reflection
        ReflectionTestUtils.setField(jwtService, "secretKey", "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 604800000L); // 7 days

        // Setup test user
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setRoles(List.of("ROLE_USER"));
    }

    @Test
    void generateToken_ValidUser_ReturnsToken() {
        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.startsWith("eyJ")); // JWT tokens start with eyJ
    }

    @Test
    void generateRefreshToken_ValidUser_ReturnsToken() {
        // When
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // Then
        assertNotNull(refreshToken);
        assertFalse(refreshToken.isEmpty());
        assertTrue(refreshToken.startsWith("eyJ"));
    }

    @Test
    void extractUsername_ValidToken_ReturnsUsername() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String extractedUsername = jwtService.extractUsername(token);

        // Then
        assertEquals("test@example.com", extractedUsername);
    }

    @Test
    void extractExpiration_ValidToken_ReturnsExpirationDate() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void isTokenValid_ValidToken_ReturnsTrue() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        boolean isValid = jwtService.isTokenValid(token, testUser);

        // Then
        assertTrue(isValid);
    }

    @Test
    void isTokenValid_DifferentUser_ReturnsFalse() {
        // Given
        String token = jwtService.generateToken(testUser);
        
        User differentUser = new User();
        differentUser.setEmail("different@example.com");

        // When
        boolean isValid = jwtService.isTokenValid(token, differentUser);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenExpired_ValidToken_ReturnsFalse() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        // Given - Create a token with very short expiration
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L); // 1 millisecond
        String token = jwtService.generateToken(testUser);
        
        // Wait for token to expire
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertTrue(isExpired);
    }

    @Test
    void extractUsername_MalformedToken_ThrowsException() {
        // Given
        String malformedToken = "invalid.token.format";

        // When & Then
        assertThrows(MalformedJwtException.class, () -> jwtService.extractUsername(malformedToken));
    }

    @Test
    void extractUsername_InvalidSignature_ThrowsException() {
        // Given
        String tokenWithInvalidSignature = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwiaWF0IjoxNjE2MjM5MDIyfQ.invalid_signature";

        // When & Then
        assertThrows(SignatureException.class, () -> jwtService.extractUsername(tokenWithInvalidSignature));
    }

    @Test
    void generateToken_WithClaims_ContainsCustomClaims() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        String username = jwtService.extractUsername(token);
        Date expiration = jwtService.extractExpiration(token);

        // Then
        assertEquals("test@example.com", username);
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void generateRefreshToken_LongerExpiration_HasLongerExpirationThanAccessToken() {
        // Given
        String accessToken = jwtService.generateToken(testUser);
        String refreshToken = jwtService.generateRefreshToken(testUser);

        // When
        Date accessExpiration = jwtService.extractExpiration(accessToken);
        Date refreshExpiration = jwtService.extractExpiration(refreshToken);

        // Then
        assertTrue(refreshExpiration.after(accessExpiration));
    }

    @Test
    void isTokenValid_NullUser_ReturnsFalse() {
        // Given
        String token = jwtService.generateToken(testUser);

        // When
        boolean isValid = jwtService.isTokenValid(token, null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_NullToken_ReturnsFalse() {
        // When
        boolean isValid = jwtService.isTokenValid(null, testUser);

        // Then
        assertFalse(isValid);
    }

    @Test
    void isTokenValid_EmptyToken_ThrowsException() {
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> jwtService.isTokenValid("", testUser));
    }

    @Test
    void generateToken_UserWithMultipleRoles_Success() {
        // Given
        testUser.setRoles(List.of("ROLE_USER", "ROLE_ADMIN"));

        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertNotNull(token);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("test@example.com", extractedUsername);
    }

    @Test
    void generateToken_UserWithNoRoles_Success() {
        // Given
        testUser.setRoles(List.of());

        // When
        String token = jwtService.generateToken(testUser);

        // Then
        assertNotNull(token);
        String extractedUsername = jwtService.extractUsername(token);
        assertEquals("test@example.com", extractedUsername);
    }

    @Test
    void tokenLifecycle_GenerateValidateExpire_WorksCorrectly() {
        // Given - Generate token
        String token = jwtService.generateToken(testUser);

        // When - Validate immediately
        boolean initiallyValid = jwtService.isTokenValid(token, testUser);
        boolean initiallyExpired = jwtService.isTokenExpired(token);

        // Then - Should be valid and not expired
        assertTrue(initiallyValid);
        assertFalse(initiallyExpired);

        // When - Check expiration date is in future
        Date expiration = jwtService.extractExpiration(token);
        
        // Then - Should expire in the future
        assertTrue(expiration.after(new Date()));
    }

    @Test
    void extractUsername_MultipleTokens_ReturnsCorrectUsernames() {
        // Given
        User user1 = new User();
        user1.setEmail("user1@example.com");
        
        User user2 = new User();
        user2.setEmail("user2@example.com");

        String token1 = jwtService.generateToken(user1);
        String token2 = jwtService.generateToken(user2);

        // When
        String extractedUser1 = jwtService.extractUsername(token1);
        String extractedUser2 = jwtService.extractUsername(token2);

        // Then
        assertEquals("user1@example.com", extractedUser1);
        assertEquals("user2@example.com", extractedUser2);
        assertNotEquals(extractedUser1, extractedUser2);
    }
}
