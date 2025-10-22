package org.example.reservationsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.reservationsystem.DTO.UserDTO;
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

    public AuthController(AuthService authService, JwtAuthenticationFilter jwtAuthenticationFilter, JwtService jwtService, UserDetailsService userDetailsService, UserRepository userRepository) {
        this.authService = authService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    // Registrierung eines neuen Benutzers und Setzen des JWT-Cookies
    @PostMapping("/register")
    public ResponseEntity<UserDTO> register(@RequestBody UserDTO userDTO, HttpServletResponse response) {
        String token = authService.register(userDTO);

        Cookie cookie = new Cookie("token", token); // JWT als HttpOnly-Cookie setzen
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24); // Cookie ist 1 Tag gültig
        response.addCookie(cookie);

        return ResponseEntity.ok(userDTO);
    }

    // Login eines Benutzers, Rückgabe von Rolle und Username
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDTO userDTO, HttpServletResponse response) {
        try {
            String token = authService.login(userDTO);

            Cookie cookie = new Cookie("token", token);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(60 * 60 * 24);
            response.addCookie(cookie);

            User user = userRepository.findByUsername(userDTO.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            return ResponseEntity.ok(Map.of(
                    "username", user.getUsername(),
                    "role", user.getRole().name()
            ));

        } catch (UsernameNotFoundException | BadCredentialsException e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Login Failed");
        }
    }

    // Authentifizierungsprüfung anhand des JWT-Cookies
    @GetMapping("/auth_check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    String token = cookie.getValue();
                    String username = jwtService.getUsername(token);
                    User user = (User) userDetailsService.loadUserByUsername(username);

                    return ResponseEntity.ok(Map.of(
                            "username", user.getUsername(),
                            "role", user.getRole().name()
                    ));
                }
            }
        }

        return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Invalid Token");
    }

    // Benutzer-Logout – Cookie wird gelöscht
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Cookie sofort ungültig
        response.addCookie(cookie);
        return ResponseEntity.ok().build();
    }
}