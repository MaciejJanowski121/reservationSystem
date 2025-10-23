package org.example.reservationsystem;

import jakarta.transaction.Transactional;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.service.ReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class ReservationServiceTest {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void testAddReservation_success() {
        // --- Given: Benutzer + Tisch ---
        User user = new User("testuser", "password", Role.ROLE_USER,
                "John Doe", "john@example.com", "+49 170 1234567");
        userRepository.saveAndFlush(user);

        // Jeśli masz konstruktor (int seats, int tableNumber) — użyj go.
        // W przeciwnym razie ustaw właściwości setterami.
        RestaurantTable table = new RestaurantTable(4, 10);
        tableRepository.saveAndFlush(table);

        // Rezerwacja: start w przyszłości, end = start + 2h
        LocalDateTime start = LocalDateTime.now().plusHours(2);
        LocalDateTime end   = start.plusHours(2);

        Reservation reservation = new Reservation(start, end);

        // --- When ---
        Reservation saved = reservationService.addReservation(reservation, 10, "testuser");

        // --- Then ---
        assertNotNull(saved.getId(), "Reservation ID should be generated");
        assertEquals("testuser", saved.getUser().getUsername());
        assertNotNull(saved.getTable());
        assertEquals(10, saved.getTable().getTableNumber());

        // użytkownik ma powiązaną rezerwację
        User reloadedUser = userRepository.findByUsername("testuser").orElseThrow();
        assertNotNull(reloadedUser.getReservation());
        assertEquals(saved.getId(), reloadedUser.getReservation().getId());

        // stolik zawiera rezerwację w swojej liście
        RestaurantTable reloadedTable = tableRepository.findTableByTableNumber(10).orElseThrow();
        assertTrue(
                reloadedTable.getReservations().stream().anyMatch(r -> r.getId().equals(saved.getId())),
                "Table should contain the saved reservation in its reservations list"
        );
    }

    @Test
    void testDeleteReservation() {
        // --- Given: Benutzer + Tisch + istniejąca rezerwacja ---
        User user = new User("tester", "password", Role.ROLE_USER,
                "Max Mustermann", "max@example.com", "+49 160 0000000");
        userRepository.saveAndFlush(user);

        RestaurantTable table = new RestaurantTable(2, 20);
        tableRepository.saveAndFlush(table);

        LocalDateTime start = LocalDateTime.now().plusHours(3);
        LocalDateTime end   = start.plusHours(1);

        Reservation toSave = new Reservation(start, end);
        Reservation saved  = reservationService.addReservation(toSave, 20, "tester");
        Long reservationId = saved.getId();

        // sanity check
        assertNotNull(reservationRepository.findById(reservationId).orElse(null));

        // --- When ---
        reservationService.deleteReservation(reservationId);
        reservationRepository.flush();

        // --- Then ---
        assertTrue(reservationRepository.findById(reservationId).isEmpty(), "Reservation should be deleted");

        // user nie ma już rezerwacji
        User reloadedUser = userRepository.findByUsername("tester").orElseThrow();
        assertNull(reloadedUser.getReservation(), "User should not reference a reservation anymore");

        // stolik nie zawiera usuniętej rezerwacji
        RestaurantTable reloadedTable = tableRepository.findTableByTableNumber(20).orElseThrow();
        assertTrue(
                reloadedTable.getReservations() == null ||
                        reloadedTable.getReservations().stream().noneMatch(r -> r.getId().equals(reservationId)),
                "Table reservations should not contain the deleted reservation"
        );
    }
}