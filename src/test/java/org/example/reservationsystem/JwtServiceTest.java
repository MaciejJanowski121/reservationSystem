package org.example.reservationsystem;

import io.jsonwebtoken.security.Keys;
import org.example.reservationsystem.JWTServices.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;


    private final String testSecretBase64 = Base64.getEncoder()
            .encodeToString(Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256).getEncoded());

    @BeforeEach
    public void setUp() {
        jwtService = new JwtService();


        ReflectionTestUtils.setField(jwtService, "secretKeyBase64", testSecretBase64);


        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L);
    }

    @Test
    public void shouldGenerateTokenAndExtractUsername() {

        UserDetails user = new User("maciej", "test-password", Collections.emptyList());


        String token = jwtService.generateToken(user);


        System.out.println("Generated token: " + token);


        assertNotNull(token, "Token should not be null!");


        String username = jwtService.extractUsername(token);
        assertEquals("maciej", username, "Extracted username should be 'maciej'");
    }

    @Test
    public void shouldValidateToken() {

        UserDetails user = new User("maciej", "test-password", Collections.emptyList());


        String token = jwtService.generateToken(user);


        boolean isValid = jwtService.isTokenValid(token, user);


        assertTrue(isValid, "Token should be valid for the given user");
    }

    @Test
    public void shouldDetectExpiredToken() throws InterruptedException {

        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1000L);

        UserDetails user = new User("maciej", "test-password", Collections.emptyList());
        String token = jwtService.generateToken(user);


        Thread.sleep(2000);


        boolean isValid = jwtService.isTokenValid(token, user);


        assertFalse(isValid, "Token should be expired");
    }
}