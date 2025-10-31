package org.example.reservationsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.DTO.ReservationViewDTO;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final ReservationService reservationService;
    private final JwtService jwtService;

    public ReservationController(ReservationService reservationService, JwtService jwtService) {
        this.reservationService = reservationService;
        this.jwtService = jwtService;
    }

    // POST /api/reservations — neue reservierung
    @PostMapping
    public ResponseEntity<ReservationViewDTO> createReservation(HttpServletRequest request,
                                                                @Valid @RequestBody ReservationRequestDTO dto) {
        final String username = extractUsernameFromToken(request);
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();


        Reservation reservation = new Reservation(dto.getStartTime(), dto.getEndTime());
        Reservation saved = reservationService.addReservation(reservation, dto.getTableNumber(), username);
        return ResponseEntity.ok(toDto(saved));
    }

    // DELETE /api/reservations/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/reservations/userReservations — angemeldete benutzer reservierungen (oder 204)
    @GetMapping("/userReservations")
    public ResponseEntity<ReservationViewDTO> getUserReservation(HttpServletRequest request) {
        final String username = extractUsernameFromToken(request);
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Reservation r = reservationService.getUserReservation(username);
        if (r == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(toDto(r));
    }

    // GET /api/reservations/all — Alle Reservierungen
    @GetMapping("/all")
    public ResponseEntity<List<ReservationViewDTO>> getAllReservations() {
        List<ReservationViewDTO> all = reservationService.getAllReservations()
                .stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(all);
    }

    // GET /api/reservations/available?start=YYYY-MM-DDTHH:mm:ss&minutes=...

    @GetMapping("/available")
    public ResponseEntity<List<TableViewDTO>> getAvailableTables(
            @RequestParam("start") String startIso,
            @RequestParam("minutes") Integer minutes) {

        // Przyjmujemy ISO_LOCAL_DATE_TIME z sekundami (frontend już wysyła z sekundami)
        LocalDateTime start = LocalDateTime.parse(startIso); // np. 2025-10-23T18:00:00
        List<TableViewDTO> free = reservationService.findAvailableTables(start, minutes);
        return ResponseEntity.ok(free);
    }

    // ------------ helpers ------------

    private String extractUsernameFromToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                try { return jwtService.getUsername(cookie.getValue()); }
                catch (Exception ignored) { }
            }
        }
        return null;
    }

    private ReservationViewDTO toDto(Reservation r) {
        return new ReservationViewDTO(
                r.getId(),
                r.getUser()  != null ? r.getUser().getUsername() : null,
                r.getTable() != null ? r.getTable().getTableNumber() : null,
                r.getStartTime(),
                r.getEndTime()
        );
    }
}