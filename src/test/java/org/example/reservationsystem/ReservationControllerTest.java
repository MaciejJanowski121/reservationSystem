package org.example.reservationsystem;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.example.reservationsystem.DTO.ReservationRequestDTO;
import org.example.reservationsystem.JWTServices.JwtService;
import org.example.reservationsystem.controller.ReservationController;
import org.example.reservationsystem.model.Reservation;
import org.example.reservationsystem.repository.ReservationRepository;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.service.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ReservationControllerTest {

    private ReservationService reservationService;
    private JwtService jwtService;
    private TableRepository tableRepository;              // <-- nowy mock
    private ReservationRepository reservationRepository;  // <-- nowy mock

    private ReservationController reservationController;

    @BeforeEach
    void setUp() {
        reservationService = mock(ReservationService.class);
        jwtService = mock(JwtService.class);
        tableRepository = mock(TableRepository.class);                    // <-- dodane
        reservationRepository = mock(ReservationRepository.class);        // <-- dodane

        reservationController = new ReservationController(
                reservationService,
                jwtService,
                tableRepository,
                reservationRepository
        );
    }

    @Test
    void createReservation_shouldReturnUnauthorized_whenTokenIsMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(null);

        ReservationRequestDTO dto = new ReservationRequestDTO();
        ResponseEntity<Reservation> response = reservationController.createReservation(request, dto);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    void createReservation_shouldReturnOk_whenValidTokenAndData() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie[] cookies = { new Cookie("token", "validToken") };
        when(request.getCookies()).thenReturn(cookies);

        Reservation reservation = new Reservation();
        ReservationRequestDTO dto = new ReservationRequestDTO();
        dto.setTableNumber(1);
        dto.setReservation(reservation);

        when(jwtService.getUsername("validToken")).thenReturn("testuser");
        when(reservationService.addReservation(any(Reservation.class), anyInt(), eq("testuser")))
                .thenReturn(reservation);

        ResponseEntity<Reservation> response = reservationController.createReservation(request, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(reservation, response.getBody());
    }

    @Test
    void deleteReservation_shouldReturnNoContent() {
        doNothing().when(reservationService).deleteReservation(1L);

        ResponseEntity<Void> response = reservationController.deleteReservation(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(reservationService).deleteReservation(1L);
    }
}