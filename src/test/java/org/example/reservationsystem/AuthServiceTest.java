package org.example.reservationsystem;

import org.example.reservationsystem.DTO.UserDTO;
import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserDTO userDTO;
    private User user;

    @BeforeEach
    void setUp() {
        userDTO = new UserDTO("testuser", "password123");
        user = new User("testuser", "encodedPassword", Role.ROLE_USER);
    }

    @Test
    void testRegister_shouldReturnToken() {
        // Arrange
        when(passwordEncoder.encode(userDTO.getPassword())).thenReturn("encodedPassword");
        when(jwtService.generateToken(any(User.class))).thenReturn("mockToken");

        // Act
        String token = authService.register(userDTO);

        // Assert
        assertEquals("mockToken", token);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testLogin_shouldReturnToken_ifCredentialsAreCorrect() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mockToken");

        // Act
        String token = authService.login(userDTO);

        // Assert
        assertEquals("mockToken", token);
        verify(jwtService).generateToken(user);
    }

    @Test
    void testLogin_shouldThrowException_ifUserNotFound() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> authService.login(userDTO));
    }

    @Test
    void testLogin_shouldThrowException_ifPasswordInvalid() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(userDTO));
    }
}