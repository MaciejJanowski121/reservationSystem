package org.example.reservationsystem.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.example.reservationsystem.DTO.AuthUserDTO;
import org.example.reservationsystem.DTO.UserLoginDTO;
import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    private void writeAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from("token", token)
                .httpOnly(true)
                .secure(false)          // USTAW NA true W HTTPS
                .sameSite("Lax")        // Dla cross-site i HTTPS -> "None"
                .path("/")
                .maxAge(60L * 60 * 24)  // 1 dzień
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
    }

    @PostMapping("/register")
    public ResponseEntity<AuthUserDTO> register(@Valid @RequestBody UserRegisterDTO userDTO,
                                                HttpServletResponse response) {
        try {
            String token = authService.register(userDTO);
            writeAuthCookie(response, token);

            AuthUserDTO body = new AuthUserDTO(
                    userDTO.getUsername(),
                    "ROLE_USER",                 // nowy użytkownik
                    userDTO.getFullName(),
                    userDTO.getEmail(),
                    userDTO.getPhone()
            );
            return ResponseEntity.ok(body);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(409).build(); // 409 Conflict (np. duplikat username/email)
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody UserLoginDTO userDTO,
                                   HttpServletResponse response) {
        try {
            String token = authService.login(userDTO);
            writeAuthCookie(response, token);

            User user = userRepository.findByUsername(userDTO.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            AuthUserDTO body = new AuthUserDTO(
                    user.getUsername(),
                    user.getRole().name(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone()
            );
            return ResponseEntity.ok(body);
        } catch (UsernameNotFoundException | BadCredentialsException e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Login Failed");
        }
    }

    @GetMapping("/auth_check")
    public ResponseEntity<?> checkAuth(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Invalid Token");
        }

        String token = null;
        for (var c : cookies) {
            if ("token".equals(c.getName())) {
                token = c.getValue();
                break;
            }
        }
        if (token == null) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Invalid Token");
        }

        try {
            String username = jwtService.getUsername(token);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found"));

            AuthUserDTO body = new AuthUserDTO(
                    user.getUsername(),
                    user.getRole().name(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getPhone()
            );
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            return ResponseEntity.status(HttpServletResponse.SC_UNAUTHORIZED).body("Invalid Token");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("token", "")
                .httpOnly(true)
                .secure(false)      // true w HTTPS
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok().build();
    }
}