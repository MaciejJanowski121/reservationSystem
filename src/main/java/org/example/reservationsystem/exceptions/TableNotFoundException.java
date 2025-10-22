package org.example.reservationsystem.exceptions;

public class TableNotFoundException extends RuntimeException{
    public TableNotFoundException(String message) {
        super(message);
    }
}
