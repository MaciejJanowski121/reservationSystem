package org.example.reservationsystem.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import org.example.reservationsystem.exceptions.TableNotFoundException;
import org.example.reservationsystem.exceptions.UserNotFoundException;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    // Zeitregeln: min. 30 Min, max. 5 Std, Standarddauer 2 Std
    private static final Duration MIN_DURATION      = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION      = Duration.ofHours(5);
    private static final Duration DEFAULT_DURATION  = Duration.ofHours(2);

    private final ReservationRepository reservationRepository;
    private final TableRepository       tableRepository;
    private final UserRepository        userRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              TableRepository tableRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository       = tableRepository;
        this.userRepository        = userRepository;
    }

    /**
     * Fügt eine neue Reservierung für Benutzer und Tisch hinzu.
     *
     * Regeln:
     * - Ein Benutzer darf nur 1 aktive Reservierung haben.
     * - Wenn endTime fehlt, wird automatisch startTime + 2 Std gesetzt.
     * - Gültige Dauer: 30–300 Minuten, startTime in der Zukunft, endTime > startTime.
     * - Zeitüberschneidungen werden in der DB geprüft (Berühren der Grenzen ist erlaubt).
     *
     * @param reservation  Reservierungsdaten (mit startTime/endTime)
     * @param tableNumber  Tischnummer
     * @param username     Benutzername (aus JWT)
     * @return gespeicherte Reservierung
     */
    public Reservation addReservation(Reservation reservation, int tableNumber, String username) {
        // Benutzer auflösen und Regel „nur 1 aktive Reservierung“ prüfen
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (user.getReservation() != null) {
            throw new IllegalStateException("User already has a reservation.");
        }

        // Tisch auflösen
        RestaurantTable table = tableRepository.findTableByTableNumber(tableNumber)
                .orElseThrow(() -> new TableNotFoundException("Table with number " + tableNumber + " does not exist."));

        // endTime automatisch setzen, wenn nur startTime angegeben wurde
        if (reservation.getStartTime() != null && reservation.getEndTime() == null) {
            reservation.setEndTime(reservation.getStartTime().plus(DEFAULT_DURATION));
        }

        // Eingaben/Zeiten validieren
        validateReservationInput(reservation);

        // Zeitüberschneidung in der DB prüfen (newStart < existingEnd && newEnd > existingStart)
        boolean overlaps = reservationRepository
                .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                        table.getId(),
                        reservation.getEndTime(),
                        reservation.getStartTime()
                );
        if (overlaps) {
            throw new IllegalStateException("Table " + tableNumber + " is already reserved in this time slot.");
        }

        // Beziehungen setzen
        reservation.setUser(user);
        reservation.setTable(table);
        user.setReservation(reservation);

        // (optional) in-memory Konsistenz der Tisch-Reservierungsliste
        if (table.getReservations() != null) {
            table.getReservations().add(reservation);
        }

        // Speichern
        Reservation saved = reservationRepository.save(reservation);
        userRepository.save(user);
        return saved;
    }

    /**
     * Liefert die Reservierung des eingeloggten Benutzers.
     */
    public Reservation getUserReservation(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getReservation();
    }

    /**
     * Löscht eine Reservierung und räumt Relationen zu Tisch/Benutzer auf.
     */
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        // Von Tisch trennen
        RestaurantTable table = reservation.getTable();
        if (table != null && table.getReservations() != null) {
            table.getReservations().remove(reservation);
        }
        reservation.setTable(null);

        // Von Benutzer trennen
        User user = reservation.getUser();
        if (user != null) {
            user.setReservation(null);
            reservation.setUser(null);
            userRepository.save(user);
        }

        // Entfernen
        reservationRepository.delete(reservation);
    }

    /**
     * Liefert alle Reservierungen (z. B. für Admin).
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Hilfsfunktion: Benutzer per Username laden.
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));
    }

    /**
     * Validiert Pflichtfelder und Zeitregeln.
     *
     * Anforderungen:
     * - name: nicht leer
     * - startTime: vorhanden & in der Zukunft
     * - endTime: vorhanden & nach startTime
     * - Dauer: 30–300 Minuten
     */
    private void validateReservationInput(Reservation r) {
        if (r == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
        }
        if (r.getName() == null || r.getName().isBlank()) {
            throw new IllegalArgumentException("Reservation name cannot be empty.");
        }
        if (r.getStartTime() == null) {
            throw new IllegalArgumentException("Reservation must have start time.");
        }
        if (r.getEndTime() == null) {
            throw new IllegalArgumentException("Reservation must have end time.");
        }
        if (!r.getEndTime().isAfter(r.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (r.getStartTime().isBefore(now)) {
            throw new IllegalArgumentException("Reservation start time cannot be in the past.");
        }

        long minutes = Duration.between(r.getStartTime(), r.getEndTime()).toMinutes();
        if (minutes < MIN_DURATION.toMinutes() || minutes > MAX_DURATION.toMinutes()) {
            throw new IllegalArgumentException("Reservation must be between 30 minutes and 5 hours.");
        }
    }
}