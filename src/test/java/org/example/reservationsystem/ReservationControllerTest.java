package org.example.reservationsystem;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.DTO.TableViewDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.controller.ReservationController;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReservationControllerTest {

    private ReservationService reservationService;
    private JwtService jwtService;

    private ReservationController reservationController;

    @BeforeEach
    void setUp() {
        // Mocks erstellen
        reservationService = mock(ReservationService.class);
        jwtService = mock(JwtService.class);

        // Controller mit genau 2 Abhängigkeiten instanziieren
        reservationController = new ReservationController(reservationService, jwtService);
    }

    @Test
    void createReservation_shouldReturnUnauthorized_whenTokenMissing() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setTableNumber(1);
        dto.setStartTime(LocalDateTime.of(2025, 10, 30, 18, 0));
        dto.setEndTime(LocalDateTime.of(2025, 10, 30, 20, 0));

        // Act
        ResponseEntity<?> resp = reservationController.createReservation(request, dto);

        // Assert
        assertEquals(401, resp.getStatusCodeValue());
        assertNull(resp.getBody());
        verifyNoInteractions(reservationService);
    }

    @Test
    void createReservation_shouldReturnOk_whenValidTokenAndPayload() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token", "validToken") });
        when(jwtService.getUsername("validToken")).thenReturn("testuser");

        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setTableNumber(5);
        LocalDateTime start = LocalDateTime.of(2025, 10, 30, 18, 0);
        LocalDateTime end   = LocalDateTime.of(2025, 10, 30, 20, 0);
        dto.setStartTime(start);
        dto.setEndTime(end);

        Reservation saved = new Reservation(start, end);
        saved.setId(123L);

        // Service-Signatur: addReservation(Reservation, int tableNumber, String username)
        when(reservationService.addReservation(any(Reservation.class), eq(5), eq("testuser")))
                .thenReturn(saved);

        // Act
        ResponseEntity<?> resp = reservationController.createReservation(request, dto);

        // Assert
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        verify(reservationService).addReservation(any(Reservation.class), eq(5), eq("testuser"));
    }

    @Test
    void deleteReservation_shouldReturnNoContent() {
        // Arrange
        doNothing().when(reservationService).deleteReservation(42L);

        // Act
        ResponseEntity<Void> resp = reservationController.deleteReservation(42L);

        // Assert
        assertEquals(204, resp.getStatusCodeValue());
        verify(reservationService).deleteReservation(42L);
    }

    @Test
    void getUserReservation_shouldReturnUnauthorized_whenTokenMissing() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        // Act
        ResponseEntity<?> resp = reservationController.getUserReservation(request);

        // Assert
        assertEquals(401, resp.getStatusCodeValue());
        verifyNoInteractions(reservationService);
    }

    @Test
    void getUserReservation_shouldReturnOk_whenReservationExists() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token", "validToken") });
        when(jwtService.getUsername("validToken")).thenReturn("alice");

        Reservation r = new Reservation(
                LocalDateTime.of(2025, 6, 8, 18, 0),
                LocalDateTime.of(2025, 6, 8, 20, 0)
        );
        r.setId(7L);

        when(reservationService.getUserReservation("alice")).thenReturn(r);

        // Act
        ResponseEntity<?> resp = reservationController.getUserReservation(request);

        // Assert
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        verify(reservationService).getUserReservation("alice");
    }

    @Test
    void getUserReservation_shouldReturnNoContent_whenReservationMissing() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{ new Cookie("token", "validToken") });
        when(jwtService.getUsername("validToken")).thenReturn("bob");
        when(reservationService.getUserReservation("bob")).thenReturn(null);

        // Act
        ResponseEntity<?> resp = reservationController.getUserReservation(request);

        // Assert
        assertEquals(204, resp.getStatusCodeValue());
    }

    @Test
    void getAvailableTables_shouldReturnList() {
        // Arrange
        LocalDateTime start = LocalDateTime.of(2025, 10, 30, 18, 0);
        when(reservationService.findAvailableTables(start, 120))
                .thenReturn(List.of(new TableViewDTO(10L, 3, 2)));

        // Act
        ResponseEntity<List<TableViewDTO>> resp =
                reservationController.getAvailableTables("2025-10-30T18:00:00", 120);

        // Assert
        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertEquals(1, resp.getBody().size());
        assertEquals(3, resp.getBody().get(0).getTableNumber());
        assertEquals(2, resp.getBody().get(0).getNumberOfSeats());
    }}