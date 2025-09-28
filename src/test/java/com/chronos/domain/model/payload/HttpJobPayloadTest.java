package com.chronos.domain.model.payload;

import com.chronos.domain.model.enums.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HttpJobPayloadTest {

    private HttpJobPayload httpJobPayload;

    @BeforeEach
    void setUp() {
        httpJobPayload = new HttpJobPayload();
    }

    @Test
    void validate_ValidHttpJob_Success() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/get");
        httpJobPayload.setHttpMethod("GET");

        // When & Then
        assertDoesNotThrow(() -> httpJobPayload.validate(JobType.HTTP));
    }

    @Test
    void validate_ValidHttpJobWithHeaders_Success() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/post");
        httpJobPayload.setHttpMethod("POST");
        httpJobPayload.setHttpHeaders(Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer token123"
        ));
        httpJobPayload.setHttpBody("{\"test\": \"data\"}");

        // When & Then
        assertDoesNotThrow(() -> httpJobPayload.validate(JobType.HTTP));
    }

    @Test
    void validate_InvalidJobType_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/get");
        httpJobPayload.setHttpMethod("GET");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.SCRIPT)
        );
        
        assertEquals("Invalid job type for HttpJobPayload: SCRIPT", exception.getMessage());
    }

    @Test
    void validate_NullUrl_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl(null);
        httpJobPayload.setHttpMethod("GET");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.HTTP)
        );
        
        assertEquals("HTTP URL is required for HTTP jobs", exception.getMessage());
    }

    @Test
    void validate_EmptyUrl_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl("");
        httpJobPayload.setHttpMethod("GET");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.HTTP)
        );
        
        assertEquals("HTTP URL is required for HTTP jobs", exception.getMessage());
    }

    @Test
    void validate_BlankUrl_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl("   ");
        httpJobPayload.setHttpMethod("GET");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.HTTP)
        );
        
        assertEquals("HTTP URL is required for HTTP jobs", exception.getMessage());
    }

    @Test
    void validate_NullMethod_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/get");
        httpJobPayload.setHttpMethod(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.HTTP)
        );
        
        assertEquals("HTTP method is required for HTTP jobs", exception.getMessage());
    }

    @Test
    void validate_EmptyMethod_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/get");
        httpJobPayload.setHttpMethod("");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.HTTP)
        );
        
        assertEquals("HTTP method is required for HTTP jobs", exception.getMessage());
    }

    @Test
    void validate_BlankMethod_ThrowsException() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/get");
        httpJobPayload.setHttpMethod("   ");

        // When & Then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> httpJobPayload.validate(JobType.HTTP)
        );
        
        assertEquals("HTTP method is required for HTTP jobs", exception.getMessage());
    }

    @Test
    void validate_AllHttpMethods_Success() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/test");
        String[] validMethods = {"GET", "POST", "PUT", "DELETE", "PATCH"};

        // When & Then
        for (String method : validMethods) {
            httpJobPayload.setHttpMethod(method);
            assertDoesNotThrow(() -> httpJobPayload.validate(JobType.HTTP),
                "Method " + method + " should be valid");
        }
    }

    @Test
    void validate_ValidUrlFormats_Success() {
        // Given
        httpJobPayload.setHttpMethod("GET");
        String[] validUrls = {
            "https://httpbin.org/get",
            "http://localhost:8080/api/test",
            "https://api.example.com/v1/users",
            "http://192.168.1.1:3000/health"
        };

        // When & Then
        for (String url : validUrls) {
            httpJobPayload.setHttpUrl(url);
            assertDoesNotThrow(() -> httpJobPayload.validate(JobType.HTTP),
                "URL " + url + " should be valid");
        }
    }

    @Test
    void setHttpHeaders_ValidHeaders_Success() {
        // Given
        Map<String, String> headers = Map.of(
            "Content-Type", "application/json",
            "Authorization", "Bearer token123",
            "User-Agent", "ChronosScheduler/1.0",
            "Accept", "application/json"
        );

        // When
        httpJobPayload.setHttpHeaders(headers);

        // Then
        assertEquals(headers, httpJobPayload.getHttpHeaders());
        assertEquals("application/json", httpJobPayload.getHttpHeaders().get("Content-Type"));
        assertEquals("Bearer token123", httpJobPayload.getHttpHeaders().get("Authorization"));
    }

    @Test
    void setHttpBody_ValidBody_Success() {
        // Given
        String jsonBody = "{\"name\": \"test\", \"value\": 123, \"active\": true}";

        // When
        httpJobPayload.setHttpBody(jsonBody);

        // Then
        assertEquals(jsonBody, httpJobPayload.getHttpBody());
    }

    @Test
    void setHttpBody_EmptyBody_Success() {
        // Given
        String emptyBody = "";

        // When
        httpJobPayload.setHttpBody(emptyBody);

        // Then
        assertEquals(emptyBody, httpJobPayload.getHttpBody());
    }

    @Test
    void setHttpBody_NullBody_Success() {
        // Given & When
        httpJobPayload.setHttpBody(null);

        // Then
        assertNull(httpJobPayload.getHttpBody());
    }

    @Test
    void equals_SameContent_ReturnsTrue() {
        // Given
        HttpJobPayload payload1 = new HttpJobPayload();
        payload1.setHttpUrl("https://httpbin.org/get");
        payload1.setHttpMethod("GET");
        payload1.setHttpHeaders(Map.of("Content-Type", "application/json"));

        HttpJobPayload payload2 = new HttpJobPayload();
        payload2.setHttpUrl("https://httpbin.org/get");
        payload2.setHttpMethod("GET");
        payload2.setHttpHeaders(Map.of("Content-Type", "application/json"));

        // When & Then
        assertEquals(payload1, payload2);
        assertEquals(payload1.hashCode(), payload2.hashCode());
    }

    @Test
    void equals_DifferentContent_ReturnsFalse() {
        // Given
        HttpJobPayload payload1 = new HttpJobPayload();
        payload1.setHttpUrl("https://httpbin.org/get");
        payload1.setHttpMethod("GET");

        HttpJobPayload payload2 = new HttpJobPayload();
        payload2.setHttpUrl("https://httpbin.org/post");
        payload2.setHttpMethod("POST");

        // When & Then
        assertNotEquals(payload1, payload2);
    }

    @Test
    void toString_ContainsAllFields() {
        // Given
        httpJobPayload.setHttpUrl("https://httpbin.org/test");
        httpJobPayload.setHttpMethod("POST");
        httpJobPayload.setHttpHeaders(Map.of("Content-Type", "application/json"));
        httpJobPayload.setHttpBody("{\"test\": true}");

        // When
        String toString = httpJobPayload.toString();

        // Then
        assertTrue(toString.contains("https://httpbin.org/test"));
        assertTrue(toString.contains("POST"));
        assertTrue(toString.contains("Content-Type"));
        assertTrue(toString.contains("{\"test\": true}"));
    }
}
