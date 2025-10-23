package org.example.reservationsystem.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.DTO.ReservationViewDTO;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReservationController.class);

    // Dozwolony zakres minut (dla endpointu /available)
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

    // ---------------------------------------------------------------------
    // POST /api/reservations — utwórz rezerwację z DTO (tableNumber, start, end)
    // Zwraca: ReservationViewDTO (lekki payload, bez encji i proxy)
    // ---------------------------------------------------------------------
    @PostMapping
    public ResponseEntity<ReservationViewDTO> createReservation(HttpServletRequest request,
                                                                @RequestBody ReservationRequestDTO dto) {
        LOGGER.info("Reservierungsanfrage erhalten");
        final String username = extractUsernameFromToken(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        final int tableNumber = dto.getTableNumber();
        final LocalDateTime start = dto.getStartTime();
        final LocalDateTime end   = dto.getEndTime(); // może być null — serwis sam ustawi domyślne +2h

        // budujemy encję tylko z czasami
        Reservation reservation = new Reservation(start, end);

        try {
            Reservation saved = reservationService.addReservation(reservation, tableNumber, username);
            return ResponseEntity.ok(toDto(saved));
        } catch (IllegalStateException e) {
            // np. kolizja czasu lub użytkownik ma już rezerwację
            LOGGER.warn("Reservierung fehlgeschlagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            LOGGER.error("Fehler bei der Erstellung der Reservierung: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---------------------------------------------------------------------
    // DELETE /api/reservations/{id} — usuń rezerwację
    // ---------------------------------------------------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReservation(@PathVariable Long id) {
        reservationService.deleteReservation(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // GET /api/reservations/userReservations — rezerwacja zalogowanego użytkownika
    // Zwraca: 200 + ReservationViewDTO lub 204, jeśli brak rezerwacji
    // ---------------------------------------------------------------------
    @GetMapping("/userReservations")
    public ResponseEntity<ReservationViewDTO> getUserReservation(HttpServletRequest request) {
        final String username = extractUsernameFromToken(request);
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Reservation r = reservationService.getUserReservation(username);
            if (r == null) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(toDto(r));
        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen der Benutzerreservierung: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---------------------------------------------------------------------
    // GET /api/reservations/all — wszystkie rezerwacje (np. dla admina)
    // Zwraca listę DTO (nie encji)
    // ---------------------------------------------------------------------
    @GetMapping("/all")
    public ResponseEntity<List<ReservationViewDTO>> getAllReservations() {
        try {
            List<ReservationViewDTO> all = reservationService.getAllReservations()
                    .stream()
                    .map(this::toDto)
                    .toList();
            return ResponseEntity.ok(all);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Abrufen aller Reservierungen: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---------------------------------------------------------------------
    // GET /api/reservations/available?start=...&minutes=...
    // Zwraca TYLKO wolne stoliki jako lekkie DTO (brak relacji/kolizji proxy)
    // ---------------------------------------------------------------------
    @GetMapping("/available")
    public ResponseEntity<List<TableViewDTO>> getAvailableTables(
            @RequestParam("start") String startParam,
            @RequestParam("minutes") Integer minutes
    ) {
        try {
            LocalDateTime start = parseFlexibleDateTime(startParam);
            final int clamped = clampMinutes(minutes);
            final LocalDateTime end = start.plusMinutes(clamped);

            List<TableViewDTO> available = tableRepository.findAll().stream()
                    .filter(table -> !reservationRepository
                            .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                                    table.getId(), end, start))
                    .map(this::toDto)
                    .toList();

            return ResponseEntity.ok(available);
        } catch (DateTimeParseException ex) {
            // błędny format daty — frontend dostaje 400 i pustą listę
            return ResponseEntity.badRequest().body(List.of());
        } catch (Exception e) {
            LOGGER.error("Fehler beim Ermitteln verfügbarer Tische: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ---------------------------------------------------------------------
    // Mappers → Encje -> DTO (żadnej serializacji encji na zewnątrz)
    // ---------------------------------------------------------------------
    private ReservationViewDTO toDto(Reservation r) {
        return new ReservationViewDTO(
                r.getId(),
                r.getUser()  != null ? r.getUser().getUsername() : null,
                r.getTable() != null ? r.getTable().getTableNumber() : null,
                r.getStartTime(),
                r.getEndTime()
        );
    }

    private TableViewDTO toDto(RestaurantTable t) {
        return new TableViewDTO(
                t.getId(),
                t.getTableNumber(),
                t.getNumberOfSeats()
        );
    }




    // ---------------------------------------------------------------------
    // Parsowanie czasu: akceptujemy "yyyy-MM-dd'T'HH:mm[:ss][.SSS]" oraz ISO z offsetem/Z
    // ---------------------------------------------------------------------
    private static LocalDateTime parseFlexibleDateTime(String raw) {
        if (raw == null) throw new DateTimeParseException("null", "", 0);

        String norm = raw.trim().replace(' ', 'T');
        if ((norm.startsWith("\"") && norm.endsWith("\"")) || (norm.startsWith("'") && norm.endsWith("'"))) {
            norm = norm.substring(1, norm.length() - 1);
        }

        var flex = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm")
                .optionalStart().appendPattern(":ss").optionalEnd()
                .optionalStart().appendPattern(".SSS").optionalEnd()
                .toFormatter(Locale.ROOT);

        try {
            return LocalDateTime.parse(norm, flex);
        } catch (DateTimeParseException ignore) { }

        try {
            OffsetDateTime odt = OffsetDateTime.parse(norm);
            return odt.toLocalDateTime();
        } catch (DateTimeParseException ignore) { }

        try {
            Instant inst = Instant.parse(norm);
            return LocalDateTime.ofInstant(inst, ZoneId.systemDefault());
        } catch (DateTimeParseException ignore) { }

        throw new DateTimeParseException("Unsupported datetime format", norm, 0);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private String extractUsernameFromToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("token".equals(cookie.getName())) {
                try {
                    return jwtService.getUsername(cookie.getValue());
                } catch (Exception ignored) { }
            }
        }
        return null;
    }

    private static int clampMinutes(Integer minutes) {
        if (minutes == null) return MIN_MINUTES;
        if (minutes < MIN_MINUTES) return MIN_MINUTES;
        if (minutes > MAX_MINUTES) return MAX_MINUTES;
        return minutes;
    }
}