package org.example.reservationsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.service.ReservationService;
import org.example.reservationsystem.repository.TableRepository;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final Logger logger = LoggerFactory.getLogger(ReservationController.class);

    private final ReservationService reservationService;
    private final JwtService jwtService;
    private final TableRepository tableRepository;

    @Autowired
    public ReservationController(ReservationService reservationService,
                                 JwtService jwtService,
                                 TableRepository tableRepository) {
        this.reservationService = reservationService;
        this.jwtService = jwtService;
        this.tableRepository = tableRepository;
    }

    // Neue Reservierung erstellen
    @PostMapping
    public ResponseEntity<Reservation> createReservation(HttpServletRequest request,
                                                         @RequestBody ReservationRequestDTO reservationRequestDTO) {
        logger.info("Reservierungsanfrage erhalten");

        String username = extractUsernameFromToken(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        int tableNumber = reservationRequestDTO.getTableNumber();
        Reservation reservation = reservationRequestDTO.getReservation();

        try {
            Reservation newReservation = reservationService.addReservation(reservation, tableNumber, username);
            logger.info("Reservierung erfolgreich gespeichert");
            return ResponseEntity.ok(newReservation);
        } catch (IllegalStateException e) {
            logger.warn("Reservierung fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
        } catch (Exception e) {
            logger.error("Fehler bei der Erstellung der Reservierung: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    // Reservierung löschen
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // Aktuelle Reservierung des eingeloggten Benutzers abrufen
    @GetMapping("/userReservations")
    public ResponseEntity<Reservation> getUserReservation(HttpServletRequest request) {
        String username = extractUsernameFromToken(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Reservation reservation = reservationService.getUserReservation(username);
            if (reservation != null) {
                return ResponseEntity.ok(reservation);
            } else {
                return ResponseEntity.noContent().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Alle Reservierungen abrufen (Admin)
    @GetMapping("/all")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        try {
            List<Reservation> allReservations = reservationService.getAllReservations();
            return ResponseEntity.ok(allReservations);
        } catch (Exception e) {
            logger.error("Fehler beim Abrufen aller Reservierungen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Verfügbare Tische für ein Zeitfenster abrufen
    @GetMapping("/available")
    public ResponseEntity<List<RestaurantTable>> getAvailableTables(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start) {

        try {
            LocalDateTime end = start.plusHours(2); // rezerwacje zawsze 2h

            List<RestaurantTable> allTables = tableRepository.findAll();

            List<RestaurantTable> available = allTables.stream()
                    .filter(table -> {
                        List<Reservation> existingReservations = table.getReservations();
                        if (existingReservations == null || existingReservations.isEmpty()) {
                            return true;
                        }

                        // wolny jeśli BRAK overlap
                        return existingReservations.stream().noneMatch(existing -> {
                            LocalDateTime existingStart = existing.getStartTime();
                            LocalDateTime existingEnd = existing.getEndTime();
                            return start.isBefore(existingEnd) && end.isAfter(existingStart);
                        });
                    })
                    .toList();

            return ResponseEntity.ok(available);

        } catch (Exception e) {
            logger.error("Fehler beim Ermitteln verfügbarer Tische: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Hilfsmethode JWT → Username
    private String extractUsernameFromToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                try {
                    return jwtService.getUsername(cookie.getValue());
                } catch (Exception e) {
                    return null;
                }
            }
        }
        return null;
    }
}