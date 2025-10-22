package org.example.reservationsystem.service;

import org.example.reservationsystem.DTO.UserDTO;
import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtAuthenticationFilter jwtAuthenticationFilter,JwtService jwtService ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
    }


    public String register(UserDTO userDTO) {
        System.out.println("Registering user: " + userDTO.getUsername());

        String encodedPassword = passwordEncoder.encode(userDTO.getPassword());
        User newUser = new User(userDTO.getUsername(), encodedPassword, Role.ROLE_USER);
        userRepository.save(newUser);
        String token = jwtService.generateToken(newUser);
        return token;
    }
    public String login(UserDTO userDTO) {
        User user = userRepository.findByUsername(userDTO.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!passwordEncoder.matches(userDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        return jwtService.generateToken(user);
    }
}


