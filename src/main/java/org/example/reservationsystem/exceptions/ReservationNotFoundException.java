package org.example.reservationsystem.exceptions;

public class ReservationNotFoundException  extends RuntimeException{
    public ReservationNotFoundException(String message) {
        super(message);
    }
}
