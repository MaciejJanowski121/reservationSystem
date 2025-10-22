package org.example.reservationsystem;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUser() {
        User user = new User("john", "encodedPassword", null);
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        var result = userService.loadUserByUsername("john");

        assertEquals("john", result.getUsername());
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("unknown"));
    }

    @Test
    void changePassword_CorrectOldPassword_ChangesPassword() {
        User user = new User("john", "oldEncoded", null);
        ChangePasswordDTO dto = new ChangePasswordDTO("old", "new");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "oldEncoded")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("newEncoded");

        userService.changePassword("john", dto);

        verify(userRepository).save(user);
        assertEquals("newEncoded", user.getPassword());
    }

    @Test
    void changePassword_WrongOldPassword_ThrowsException() {
        User user = new User("john", "oldEncoded", null);
        ChangePasswordDTO dto = new ChangePasswordDTO("wrong", "new");

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "oldEncoded")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> userService.changePassword("john", dto));
    }
}