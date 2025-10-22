package org.example.reservationsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationController.class);

    // Dauergrenzen in Minuten (30–300)
    private static final int MIN_MINUTES = 30;
    private static final int MAX_MINUTES = 300;

    private final ReservationService     reservationService;
    private final JwtService             jwtService;
    private final TableRepository        tableRepository;
    private final ReservationRepository  reservationRepository;

    @Autowired
    public ReservationController(ReservationService reservationService,
                                 JwtService jwtService,
                                 TableRepository tableRepository,
                                 ReservationRepository reservationRepository) {
        this.reservationService    = reservationService;
        this.jwtService            = jwtService;
        this.tableRepository       = tableRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Erstellt eine neue Reservierung.
     * Erwartet: JWT im Cookie "token", sowie ReservationRequestDTO (Tischnummer + Reservierung).
     *
     * @param request HTTP-Request (für JWT-Cookie)
     * @param dto      Daten der Reservierungsanfrage
     * @return 200 OK mit gespeicherter Reservierung, 401 wenn nicht eingeloggt,
     *         409 bei Zeitüberschneidung, 500 bei unerwarteten Fehlern.
     */
    @PostMapping
    public ResponseEntity<Reservation> createReservation(HttpServletRequest request,
                                                         @RequestBody ReservationRequestDTO dto) {
        LOGGER.info("Reservierungsanfrage erhalten");

        final String username = extractUsernameFromToken(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final int tableNumber = dto.getTableNumber();
        final Reservation reservation = dto.getReservation();

        try {
            Reservation saved = reservationService.addReservation(reservation, tableNumber, username);
            LOGGER.info("Reservierung erfolgreich gespeichert");
            return ResponseEntity.ok(saved);
        } catch (IllegalStateException e) {
            // z. B. Zeitüberschneidung
            LOGGER.warn("Reservierung fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            LOGGER.error("Fehler bei der Erstellung der Reservierung: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Löscht eine Reservierung.
     *
     * @param id ID der Reservierung
     * @return 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Liefert die Reservierung des eingeloggten Benutzers.
     *
     * @param request HTTP-Request (für JWT-Cookie)
     * @return 200 OK mit Reservierung, 204 No Content wenn keine vorhanden,
     *         401 wenn nicht eingeloggt, 500 bei Fehler.
     */
    @GetMapping("/userReservations")
    public ResponseEntity<Reservation> getUserReservation(HttpServletRequest request) {
        final String username = extractUsernameFromToken(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            Reservation reservation = reservationService.getUserReservation(username);
            if (reservation != null) {
                return ResponseEntity.ok(reservation);
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen der Benutzerreservierung: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Liefert alle Reservierungen (z. B. für Admin-Zwecke).
     *
     * @return 200 OK mit Liste
     */
    @GetMapping("/all")
    public ResponseEntity<List<Reservation>> getAllReservations() {
        try {
            List<Reservation> all = reservationService.getAllReservations();
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen aller Reservierungen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Liefert alle verfügbaren Tische für ein Zeitfenster.
     * Es werden ausschließlich freie Tische zurückgegeben (keine Zeitüberschneidung).
     *
     * Request-Parameter:
     * - start: ISO-Datum/Zeit (z. B. 2025-10-22T18:00:00)
     * - minutes: gewünschte Dauer in Minuten (wird auf 30–300 geklemmt)
     *
     * @return 200 OK mit Liste verfügbarer Tische, 500 bei Fehler.
     */
    @GetMapping("/available")
    public ResponseEntity<List<RestaurantTable>> getAvailableTables(
            @RequestParam("start")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("minutes") Integer minutes) {
        try {
            final int clamped = clampMinutes(minutes);
            final LocalDateTime end = start.plusMinutes(clamped);

            // frei, wenn KEINE Überschneidung (newStart < existingEnd && newEnd > existingStart -> false)
            List<RestaurantTable> available = tableRepository.findAll().stream()
                    .filter(table -> !reservationRepository
                            .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                                    table.getId(), end, start))
                    .toList();

            return ResponseEntity.ok(available);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Ermitteln verfügbarer Tische: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ------------------------------------------------------------
    // Hilfsfunktionen
    // ------------------------------------------------------------

    /** Liest den Benutzernamen aus dem JWT-Cookie "token". */
    private String extractUsernameFromToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                try {
                    return jwtService.getUsername(cookie.getValue());
                } catch (Exception ignored) {
                    // ungültiges/abgelaufenes Token
                }
            }
        }
        return null;
    }

    /** Begrenzt die angefragte Dauer auf 30–300 Minuten. */
    private static int clampMinutes(Integer minutes) {
        if (minutes == null) return MIN_MINUTES; // defensiver Default
        if (minutes < MIN_MINUTES) return MIN_MINUTES;
        if (minutes > MAX_MINUTES) return MAX_MINUTES;
        return minutes;
    }
}