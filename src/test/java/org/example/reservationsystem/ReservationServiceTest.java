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
import java.util.Optional;

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
        // Given: Benutzer + Tisch
        User user = new User("testuser", "password", Role.ROLE_USER);
        userRepository.save(user);

        RestaurantTable table = new RestaurantTable(4, 10);
        tableRepository.save(table);

        Reservation reservation = new Reservation();
        reservation.setReservationTime(LocalDateTime.of(2025, 12, 10, 16, 30));
        reservation.setName("John");
        reservation.setEmail("john@example.com");
        reservation.setPhone("123456789");

        // When
        Reservation savedReservation = reservationService.addReservation(reservation, 10, "testuser");

        // Then
        assertNotNull(savedReservation.getId());
        assertEquals("John", savedReservation.getName());
        assertEquals("testuser", savedReservation.getUser().getUsername());
        assertTrue(savedReservation.getTable().isReserved());
    }

    @Test
    void testDeleteReservation() {
        // Given: Tisch mit Reservierung
        RestaurantTable restaurantTable = new RestaurantTable(4, 20);
        tableRepository.save(restaurantTable);

        Reservation reservation = new Reservation();
        reservation.setReservationTime(LocalDateTime.of(2025, 12, 10, 16, 30));
        reservation.setName("John");
        reservation.setEmail("john@example.com");
        reservation.setPhone("123456789");
        reservation.setTable(restaurantTable);
        reservationRepository.save(reservation);

        restaurantTable.setReservation(reservation);
        restaurantTable.markAsReserved();
        tableRepository.save(restaurantTable);

        // When: löschen
        reservationService.deleteReservation(reservation.getId());
        reservationRepository.flush();

        // Then: Prüfung
        Optional<Reservation> deleted = reservationRepository.findById(reservation.getId());
        assertTrue(deleted.isEmpty());

        Optional<RestaurantTable> updatedTable = tableRepository.findById(restaurantTable.getId());
        assertTrue(updatedTable.isPresent());
        assertNull(updatedTable.get().getReservation());
        assertFalse(updatedTable.get().isReserved());
    }
}