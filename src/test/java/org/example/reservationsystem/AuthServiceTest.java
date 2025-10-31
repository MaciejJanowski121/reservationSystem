package org.example.reservationsystem;

import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
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

    // Wird im AuthService-Konstruktor injiziert, hier aber nicht aktiv verwendet
    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private UserRegisterDTO registerDTO;
    private UserLoginDTO loginDTO;
    private User user;

    @BeforeEach
    void setUp() {
        // --- Register-DTO über leeren Konstruktor + Setter ---
        registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setFullName("Test User");
        registerDTO.setEmail("testuser@example.com");
        registerDTO.setPhone("+49 170 0000000");

        // --- Login-DTO über leeren Konstruktor + Setter ---
        loginDTO = new UserLoginDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("password123");

        // --- User-Entity entsprechend dem aktuellen Konstruktor (inkl. Profildaten) ---
        user = new User(
                "testuser",
                "encodedPassword",
                Role.ROLE_USER,
                "Test User",
                "testuser@example.com",
                "+49 170 0000000"
        );
    }

    @Test
    void testRegister_shouldReturnToken() {
        // Arrange
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        // Speichern des Users simulieren
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("mockToken");

        // Act
        String token = authService.register(registerDTO);

        // Assert
        assertEquals("mockToken", token);
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(User.class));
    }

    @Test
    void testLogin_shouldReturnToken_ifCredentialsAreCorrect() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn("mockToken");

        // Act
        String token = authService.login(loginDTO);

        // Assert
        assertEquals("mockToken", token);
        verify(jwtService).generateToken(user);
    }

    @Test
    void testLogin_shouldThrowException_ifUserNotFound() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Assert
        assertThrows(UsernameNotFoundException.class, () -> authService.login(loginDTO));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void testLogin_shouldThrowException_ifPasswordInvalid() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(loginDTO));
        verify(jwtService, never()).generateToken(any());
    }
}