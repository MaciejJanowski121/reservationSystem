package org.example.reservationsystem.DTO;

public record AuthUserDTO(
        String username,
        String role,
        String fullName,
        String email,
        String phone
) {}