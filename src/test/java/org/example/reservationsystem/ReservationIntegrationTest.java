package org.example.reservationsystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ReservationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private UserRepository userRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private ReservationRepository reservationRepository;

    @Autowired private JwtService jwtService;

    private String jwtToken;
    private User testUser;
    private RestaurantTable testTable;

    @BeforeEach
    void setup() {
        // Datenbank bereinigen (Reihenfolge: Reservierungen -> Benutzer/Tische)
        reservationRepository.deleteAll();
        userRepository.deleteAll();
        tableRepository.deleteAll();

        // Benutzer anlegen
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setRole(Role.ROLE_USER);
        testUser = userRepository.save(testUser);

        // Tisch anlegen (Nummer 5)
        testTable = new RestaurantTable();
        testTable.setTableNumber(5);
        testTable.setNumberOfSeats(4);
        testTable = tableRepository.save(testTable);

        // JWT erzeugen
        jwtToken = jwtService.generateToken(testUser);
    }

    private Map<String, Object> validReservationPayload() {
        // Start/Ende morgen 18:00–20:00 (Sekunden explizit auf 0)
        LocalDateTime start = LocalDateTime.now().plusDays(1)
                .withHour(18).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime end = start.plusHours(2);

        Map<String, Object> payload = new HashMap<>();
        payload.put("tableNumber", testTable.getTableNumber());
        payload.put("startTime", start.toString()); // ISO_LOCAL_DATE_TIME mit Sekunden
        payload.put("endTime", end.toString());
        return payload;
    }

    @Test
    void createReservation_shouldReturnOk_withDtoResponse() throws Exception {
        Map<String, Object> body = validReservationPayload();

        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.tableNumber").value(testTable.getTableNumber()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    @Test
    void getUserReservation_shouldReturnDto_afterCreation() throws Exception {
        // Reservierung anlegen
        Map<String, Object> body = validReservationPayload();
        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // Eigene Reservierung abfragen
        mockMvc.perform(get("/api/reservations/userReservations")
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.tableNumber").value(testTable.getTableNumber()))
                .andExpect(jsonPath("$.startTime").exists())
                .andExpect(jsonPath("$.endTime").exists());
    }

    @Test
    void deleteReservation_shouldReturnNoContent() throws Exception {
        // Reservierung anlegen
        Map<String, Object> body = validReservationPayload();
        mockMvc.perform(post("/api/reservations")
                        .cookie(new MockCookie("token", jwtToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        // ID aus der DB holen und löschen
        Reservation created = reservationRepository.findAll().get(0);
        mockMvc.perform(delete("/api/reservations/" + created.getId())
                        .cookie(new MockCookie("token", jwtToken)))
                .andExpect(status().isNoContent());
    }
}