package org.example.reservationsystem.controller;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.DTO.UserProfileDTO;
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

    // Konstruktorinjektion
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** Liefert Profildaten des eingeloggten Benutzers. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> me(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        UserProfileDTO dto = userService.getProfile(currentUser.getUsername());
        return ResponseEntity.ok(dto);
    }

    /** Aktualisiert Profildaten (fullName, email, phone) des eingeloggten Benutzers. */
    @PutMapping("/me")
    public ResponseEntity<Void> updateMe(@AuthenticationPrincipal User currentUser,
                                         @RequestBody UserProfileDTO dto) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        userService.updateProfile(currentUser.getUsername(), dto);
        return ResponseEntity.noContent().build();
    }

    /** Passwort ändern – nur für eingeloggte Benutzer. */
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal User currentUser,
                                            @RequestBody ChangePasswordDTO changePasswordDTO) {
        if (currentUser == null) return ResponseEntity.status(401).build();
        userService.changePassword(currentUser.getUsername(), changePasswordDTO);
        return ResponseEntity.ok("Password changed successfully.");
    }
}