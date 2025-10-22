package org.example.reservationsystem.controller;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    // Konstruktor zur Injektion des UserService
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Passwort ändern – nur für eingeloggte Benutzer
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal User currentUser, // aktueller eingeloggter Benutzer
            @RequestBody ChangePasswordDTO changePasswordDTO // enthält altes und neues Passwort
    ) {
        userService.changePassword(currentUser.getUsername(), changePasswordDTO);
        return ResponseEntity.ok("Password changed successfully.");
    }
}