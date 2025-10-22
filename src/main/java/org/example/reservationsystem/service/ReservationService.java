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

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReservationService(ReservationRepository reservationRepository,
                              TableRepository tableRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }

    /**
     * Neue Reservierung für Benutzer + Tisch hinzufügen
     */
    public Reservation addReservation(Reservation reservation, int tableNumber, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Jeder Benutzer darf nur 1 aktive Reservierung haben
        if (user.getReservation() != null) {
            throw new IllegalStateException("User already has a reservation.");
        }

        RestaurantTable table = tableRepository.findTableByTableNumber(tableNumber)
                .orElseThrow(() -> new TableNotFoundException(
                        "Table with number " + tableNumber + " does not exist."));

        // --- Auto-Endzeit, falls leer ---
        if (reservation.getStartTime() != null && reservation.getEndTime() == null) {
            reservation.setEndTime(reservation.getStartTime().plusHours(2));
        }

        // --- Eingaben prüfen ---
        validateReservationInput(reservation, table);

        // --- Zeitüberschneidung prüfen ---
        for (Reservation existing : table.getReservations()) {
            boolean overlap =
                    reservation.getStartTime().isBefore(existing.getEndTime()) &&
                            reservation.getEndTime().isAfter(existing.getStartTime());

            if (overlap) {
                throw new IllegalStateException(
                        "Table " + tableNumber + " is already reserved in this time slot.");
            }
        }

        // --- Relationen setzen ---
        reservation.setUser(user);
        user.setReservation(reservation);

        reservation.setTable(table);
        table.getReservations().add(reservation);

        // --- Speichern ---
        reservation = reservationRepository.save(reservation);
        tableRepository.save(table);
        userRepository.save(user);

        return reservation;
    }

    /**
     * Hole Reservierung für eingeloggten Benutzer
     */
    public Reservation getUserReservation(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getReservation();
    }

    /**
     * Lösche Reservierung inkl. Aufräumen der Relationen
     */
    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));

        // Tisch aktualisieren
        RestaurantTable table = reservation.getTable();
        if (table != null) {
            table.getReservations().remove(reservation);
            tableRepository.save(table);
        }

        // Benutzer aktualisieren
        User user = reservation.getUser();
        if (user != null) {
            user.setReservation(null);
            reservation.setUser(null);
            userRepository.save(user);
        }

        reservation.setTable(null);
        reservationRepository.delete(reservation);
    }

    /**
     * Validierung der Eingaben
     */
    private void validateReservationInput(Reservation reservation, RestaurantTable table) {
        if (reservation == null || reservation.getName() == null || reservation.getName().isBlank()) {
            throw new IllegalArgumentException("Reservation name cannot be empty.");
        }
        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            throw new IllegalArgumentException("Reservation must have start and end time.");
        }
        if (reservation.getEndTime().isBefore(reservation.getStartTime())) {
            throw new IllegalArgumentException("Reservation end time cannot be before start time.");
        }
        if (reservation.getStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reservation start time cannot be in the past.");
        }

        long hours = Duration.between(reservation.getStartTime(), reservation.getEndTime()).toHours();
        if (hours < 1 || hours > 3) {
            throw new IllegalArgumentException("Reservation must be between 1 and 3 hours.");
        }

        if (table == null) {
            throw new IllegalArgumentException("No table provided for the reservation.");
        }
    }

    /**
     * Alle Reservierungen zurückgeben – nur Admin
     */
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /**
     * Hilfsfunktion für Benutzer
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Benutzer nicht gefunden"));
    }
}