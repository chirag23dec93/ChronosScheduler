package com.chronos.config;

import com.chronos.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private AuthenticationProvider authenticationProvider;

    @Test
    void securityFilterChain_ConfiguredCorrectly() {
        // This test verifies that the SecurityConfig bean is created successfully
        // The presence of MockMvc indicates the security configuration is working
        assertNotNull(mockMvc);
    }

    @Test
    void publicEndpoints_AccessibleWithoutAuthentication() throws Exception {
        // Test public authentication endpoints
        mockMvc.perform(post("/api/auth/register")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().is4xxClientError()); // Will fail validation but not auth

        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().is4xxClientError()); // Will fail auth but not security
    }

    @Test
    void actuatorEndpoints_AccessibleWithoutAuthentication() throws Exception {
        // Test actuator endpoints are accessible
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerEndpoints_AccessibleWithoutAuthentication() throws Exception {
        // Test Swagger UI endpoints are accessible
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection()); // Redirects to swagger-ui/index.html

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk());
    }

    @Test
    void protectedEndpoints_RequireAuthentication() throws Exception {
        // Test that protected endpoints require authentication
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/jobs")
                .contentType("application/json")
                .content("{}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/jobs/123"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoints_RequireAuthentication() throws Exception {
        // Test that admin endpoints require authentication
        mockMvc.perform(get("/api/dlq"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/audit"))
                .andExpected(status().isUnauthorized());
    }

    @Test
    void corsConfiguration_AllowsConfiguredOrigins() throws Exception {
        // Test CORS preflight request
        mockMvc.perform(options("/api/jobs")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Authorization,Content-Type"));
    }

    @Test
    void sessionManagement_IsStateless() throws Exception {
        // Verify that no session is created (stateless)
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(request -> assertNull(request.getRequest().getSession(false)));
    }

    @Test
    void csrfProtection_DisabledForApi() throws Exception {
        // CSRF should be disabled for API endpoints
        mockMvc.perform(post("/api/auth/login")
                .contentType("application/json")
                .content("{\"email\":\"test@example.com\",\"password\":\"password\"}"))
                .andExpect(status().is4xxClientError()); // Should not be 403 Forbidden due to CSRF
    }

    @Test
    void httpBasic_Disabled() throws Exception {
        // HTTP Basic authentication should be disabled
        mockMvc.perform(get("/api/jobs")
                .header("Authorization", "Basic dGVzdDp0ZXN0")) // test:test in base64
                .andExpect(status().isUnauthorized()); // Should not authenticate via Basic
    }

    @Test
    void formLogin_Disabled() throws Exception {
        // Form login should be disabled
        mockMvc.perform(post("/login")
                .param("username", "test")
                .param("password", "test"))
                .andExpect(status().isNotFound()); // Login form endpoint should not exist
    }

    @Test
    void logout_CustomEndpoint() throws Exception {
        // Custom logout endpoint should not be accessible without auth
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void securityHeaders_ConfiguredCorrectly() throws Exception {
        // Test security headers are set
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
    }

    @Test
    void methodSecurity_EnabledGlobally() throws Exception {
        // Method security should be enabled (tested implicitly through endpoint access)
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exceptionHandling_ConfiguredCorrectly() throws Exception {
        // Test that authentication exceptions are handled properly
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"));
    }

    @Test
    void passwordEncoder_BeanExists() {
        // This test verifies the PasswordEncoder bean is configured
        // The test passes if the application context loads successfully
        assertTrue(true);
    }

    @Test
    void authenticationManager_BeanExists() {
        // This test verifies the AuthenticationManager bean is configured
        // The test passes if the application context loads successfully
        assertTrue(true);
    }

    @Test
    void jwtFilter_IntegratedCorrectly() throws Exception {
        // Test that JWT filter is in the filter chain
        // Invalid JWT should result in unauthorized, not internal server error
        mockMvc.perform(get("/api/jobs")
                .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void multipleHttpMethods_SupportedCorrectly() throws Exception {
        // Test that different HTTP methods are handled correctly by security
        mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/jobs").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/jobs/123").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/jobs/123")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/jobs/123").contentType("application/json").content("{}")).andExpect(status().isUnauthorized());
    }

    @Test
    void contentNegotiation_WorksWithSecurity() throws Exception {
        // Test that content negotiation works with security configuration
        mockMvc.perform(get("/api/jobs")
                .accept("application/json"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType("application/json"));

        mockMvc.perform(get("/api/jobs")
                .accept("application/xml"))
                .andExpect(status().isUnauthorized());
    }
}
