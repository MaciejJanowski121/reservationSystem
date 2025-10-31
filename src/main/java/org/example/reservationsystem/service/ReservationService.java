package org.example.reservationsystem.service;

import jakarta.persistence.EntityNotFoundException;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.exceptions.TableNotFoundException;
import org.example.reservationsystem.exceptions.UserNotFoundException;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ReservationService {

    // Reguły czasu: min. 30 min, max. 5 h, domyślnie 2 h
    private static final Duration MIN_DURATION     = Duration.ofMinutes(30);
    private static final Duration MAX_DURATION     = Duration.ofHours(5);
    private static final Duration DEFAULT_DURATION = Duration.ofHours(2);

    // (dla endpointu /available)
    private static final int MIN_MINUTES = 30;
    private static final int MAX_MINUTES = 300;

    private final ReservationRepository reservationRepository;
    private final TableRepository tableRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              TableRepository tableRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.tableRepository = tableRepository;
        this.userRepository = userRepository;
    }


    public Reservation addReservation(Reservation reservation, int tableNumber, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        // Reguła 1:1 (na razie zostawiamy)
        if (user.getReservation() != null) {
            throw new IllegalStateException("User already has a reservation.");
        }

        RestaurantTable table = tableRepository.findTableByTableNumber(tableNumber)
                .orElseThrow(() -> new TableNotFoundException("Table with number " + tableNumber + " does not exist."));

        // Jeśli endTime brak — ustaw domyślnie start + 2h
        if (reservation.getStartTime() != null && reservation.getEndTime() == null) {
            reservation.setEndTime(reservation.getStartTime().plus(DEFAULT_DURATION));
        }

        validateReservationInput(reservation);

        // Kolizja czasu: newStart < existingEnd && newEnd > existingStart
        boolean overlaps = reservationRepository
                .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                        table.getId(),
                        reservation.getEndTime(),
                        reservation.getStartTime()
                );
        if (overlaps) {
            throw new IllegalStateException("Table " + tableNumber + " is already reserved in this time slot.");
        }


        reservation.setUser(user);
        reservation.setTable(table);
        user.setReservation(reservation);

        if (table.getReservations() != null) {
            table.getReservations().add(reservation);
        }

        Reservation saved = reservationRepository.save(reservation);
        userRepository.save(user); // utrzymanie relacji 1:1
        return saved;
    }


    @Transactional(readOnly = true)
    public Reservation getUserReservation(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return user.getReservation();
    }


    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Reservation not found"));


        RestaurantTable table = reservation.getTable();
        if (table != null && table.getReservations() != null) {
            table.getReservations().remove(reservation);
        }
        reservation.setTable(null);

        // odczep od użytkownika (1:1)
        User user = reservation.getUser();
        if (user != null) {
            user.setReservation(null);
            reservation.setUser(null);
            userRepository.save(user);
        }

        reservationRepository.delete(reservation);
    }

    /** Wszystkie rezerwacje (np. dla admina). */
    @Transactional(readOnly = true)
    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    /** Lista wolnych stolików (używane przez endpoint /available). */
    @Transactional(readOnly = true)
    public List<TableViewDTO> findAvailableTables(LocalDateTime start, Integer minutes) {
        int clamped = clampMinutes(minutes);
        LocalDateTime end = start.plusMinutes(clamped);

        return tableRepository.findAll().stream()
                .filter(table -> !reservationRepository
                        .existsByTable_IdAndStartTimeLessThanAndEndTimeGreaterThan(
                                table.getId(), end, start))
                .map(t -> new TableViewDTO(t.getId(), t.getTableNumber(), t.getNumberOfSeats()))
                .toList();
    }

    // ---------------- helpers ----------------

    private static int clampMinutes(Integer minutes) {
        if (minutes == null) return MIN_MINUTES;
        if (minutes < MIN_MINUTES) return MIN_MINUTES;
        if (minutes > MAX_MINUTES) return MAX_MINUTES;
        return minutes;
    }

    /** Validiert Pflichtfelder und Zeitregeln (30–300 Min, Zukunft, end > start, max bis 22:00). */
    private void validateReservationInput(Reservation r) {
        if (r == null) {
            throw new IllegalArgumentException("Reservation cannot be null.");
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

        // --- NEUE REGEL: Rezerwacja maks. do 22:00 ---
        LocalDateTime latestAllowed = r.getStartTime().toLocalDate().atTime(22, 0);
        if (r.getEndTime().isAfter(latestAllowed)) {
            throw new IllegalArgumentException("Reservations are only allowed until 22:00.");
        }
    }
}