package org.example.reservationsystem;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.reservationsystem.DTO.UserDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.controller.AuthController;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

    @Mock private AuthService authService;
    @Mock private JwtService jwtService;
    @Mock private UserRepository userRepository;
    @Mock private UserDetailsService userDetailsService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister() {
        UserDTO userDTO = new UserDTO("maciej", "test123");
        when(authService.register(userDTO)).thenReturn("jwt-token");

        ResponseEntity<UserDTO> result = authController.register(userDTO, response);

        assertEquals(200, result.getStatusCodeValue());
        assertEquals("maciej", result.getBody().getUsername());
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    void testLoginSuccess() {
        UserDTO userDTO = new UserDTO("maciej", "test123");
        User user = new User("maciej", "hashed-pw", Role.ROLE_USER);

        when(authService.login(userDTO)).thenReturn("jwt-token");
        when(userRepository.findByUsername("maciej")).thenReturn(Optional.of(user));

        ResponseEntity<?> result = authController.login(userDTO, response);

        assertEquals(200, result.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertEquals("maciej", body.get("username"));
        assertEquals("ROLE_USER", body.get("role"));
        verify(response).addCookie(any(Cookie.class));
    }

    @Test
    void testLoginFailure() {
        UserDTO userDTO = new UserDTO("maciej", "wrongpw");

        when(authService.login(userDTO)).thenThrow(new UsernameNotFoundException("User not found"));

        ResponseEntity<?> result = authController.login(userDTO, response);

        assertEquals(401, result.getStatusCodeValue());
        assertEquals("Login Failed", result.getBody());
    }

    @Test
    void testCheckAuthWithValidToken() {
        Cookie tokenCookie = new Cookie("token", "valid-token");
        Cookie[] cookies = new Cookie[] { tokenCookie };
        when(request.getCookies()).thenReturn(cookies);
        when(jwtService.getUsername("valid-token")).thenReturn("maciej");

        User user = new User("maciej", "pw", Role.ROLE_USER);
        when(userDetailsService.loadUserByUsername("maciej")).thenReturn(user);

        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(200, result.getStatusCodeValue());
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertEquals("maciej", body.get("username"));
        assertEquals("ROLE_USER", body.get("role"));
    }

    @Test
    void testCheckAuthWithMissingCookie() {
        when(request.getCookies()).thenReturn(null);
        ResponseEntity<?> result = authController.checkAuth(request);

        assertEquals(401, result.getStatusCodeValue());
        assertEquals("Invalid Token", result.getBody());
    }

    @Test
    void testLogout() {
        ResponseEntity<Void> result = authController.logout(response);
        assertEquals(200, result.getStatusCodeValue());
        verify(response).addCookie(argThat(cookie -> cookie.getMaxAge() == 0));
    }
}