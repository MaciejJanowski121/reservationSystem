package org.example.reservationsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtService jwtService,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    // Registrierung: nimmt UserRegisterDTO (username, password, fullName, email, phone) an
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegisterDTO userDTO, HttpServletResponse response) {
        String token = authService.register(userDTO);

        Cookie cookie = new Cookie("token", token); // JWT als HttpOnly-Cookie
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        // cookie.setSecure(true); // aktivieren, wenn HTTPS
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of(
                "username", userDTO.getUsername(),
                "fullName", userDTO.getFullName(),
                "email",    userDTO.getEmail(),
                "phone",    userDTO.getPhone()
        ));
    }

    // Login: nimmt UserLoginDTO (username, password) an
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginDTO userDTO, HttpServletResponse response) {
        try {
            String token = authService.login(userDTO);

            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24);
            // cookie.setSecure(true); // aktivieren, wenn HTTPS
            response.addCookie(cookie);

            User user = userRepository.findByUsername(userDTO.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return ResponseEntity.ok(Map.of(
                    "username", user.getUsername(),
                    "role",     user.getRole().name(),
                    "fullName", user.getFullName(),
                    "email",    user.getEmail(),
                    "phone",    user.getPhone()
            ));

        } catch (UsernameNotFoundException | BadCredentialsException e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Login Failed");
        }
    }

    // Auth-Check via JWT-Cookie
    @GetMapping("/auth_check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if ("token".equals(c.getName())) {
                    String username = jwtService.getUsername(c.getValue());
                    User user = (User) userDetailsService.loadUserByUsername(username);
                    return ResponseEntity.ok(Map.of(
                            "username", user.getUsername(),
                            "role",     user.getRole().name(),
                            "fullName", user.getFullName(),
                            "email",    user.getEmail(),
                            "phone",    user.getPhone()
                    ));
                }
            }
        }
        return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Invalid Token");
    }

    // Logout: Cookie löschen
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}