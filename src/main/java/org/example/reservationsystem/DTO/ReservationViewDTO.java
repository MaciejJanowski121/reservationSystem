package org.example.reservationsystem.DTO;

import java.time.LocalDateTime;

public record ReservationViewDTO(
        Long id,
        String username,
        Integer tableNumber,
        LocalDateTime startTime,
        LocalDateTime endTime
) {}