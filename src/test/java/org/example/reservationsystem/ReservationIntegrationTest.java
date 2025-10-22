package org.example.reservationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.example.reservationsystem.JWTServices.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private JwtService jwtService;

    private String jwtToken;
    private User testUser;
    private RestaurantTable testTable;

    @BeforeEach
    public void setup() {
        // Usuń wszystkie rezerwacje i przywróć powiązane stoliki
        for (Reservation reservation : reservationRepository.findAll()) {
            RestaurantTable table = reservation.getTable();
            if (table != null) {
                table.setReservation(null);
                table.setReserved(false);
                tableRepository.save(table);
            }

            reservation.setTable(null);
            reservation.setUser(null);
            reservationRepository.save(reservation);
        }

        reservationRepository.deleteAll();

        // Usuń użytkowników i stoliki dopiero po zerwaniu powiązań
        userRepository.deleteAll();
        tableRepository.deleteAll();

        // Stwórz testowego użytkownika
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole(Role.ROLE_USER);
        testUser = userRepository.save(testUser);

        // Stwórz testowy stolik
        testTable = new RestaurantTable();
        testTable.setTableNumber(5);
        testTable.setNumberOfSeats(4);
        testTable.setReserved(false);
        testTable = tableRepository.save(testTable);

        // Wygeneruj JWT
        jwtToken = jwtService.generateToken(testUser);
    }

    @Test
    public void testCreateReservation() throws Exception {
        Map<String, Object> reservationData = new HashMap<>();
        Map<String, Object> reservation = new HashMap<>();
        reservation.put("id", null);
        reservation.put("name", "Test Reservation");
        reservation.put("email", "test@example.com");
        reservation.put("phone", "123456789");
        reservation.put("reservationTime", LocalDateTime.now().plusDays(1).toString());
        reservation.put("table", null);

        reservationData.put("reservation", reservation);
        reservationData.put("tableNumber", 5);

        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reservationData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Reservation"));
    }

    @Test
    public void testGetUserReservation() throws Exception {
        // Tworzymy rezerwację najpierw
        testCreateReservation();

        mockMvc.perform(get("/api/reservations/userReservations")
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Test Reservation"));
    }

    @Test
    public void testDeleteReservation() throws Exception {
        // Najpierw utwórz rezerwację
        testCreateReservation();

        Reservation created = reservationRepository.findAll().get(0);

        mockMvc.perform(delete("/api/reservations/" + created.getId())
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isNoContent());
    }
}