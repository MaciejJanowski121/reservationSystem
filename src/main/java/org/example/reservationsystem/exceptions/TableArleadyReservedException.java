package org.example.reservationsystem.exceptions;

public class TableArleadyReservedException extends RuntimeException{
    public TableArleadyReservedException(String message) {
        super(message);
    }
}
