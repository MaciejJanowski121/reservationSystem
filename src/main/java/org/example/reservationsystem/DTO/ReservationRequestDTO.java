package org.example.reservationsystem.DTO;

import org.example.reservationsystem.model.Reservation;

public class ReservationRequestDTO {

    private Reservation reservation;
    private int tableNumber;

    public ReservationRequestDTO(Reservation reservation, int tableNumber) {
        this.reservation = reservation;
        this.tableNumber = tableNumber;
    }

    public ReservationRequestDTO() {};

    // Getter und Setter
    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public void setTableNumber(int tableNumber) {
        this.tableNumber = tableNumber;
    }
}