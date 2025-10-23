package org.example.reservationsystem.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionsHandler {
    @ExceptionHandler(ReservationNotFoundException.class)
    public String reservationNotFoundExceptionHandler(ReservationNotFoundException exception){
        return exception.getMessage();
    }

    @ExceptionHandler(TableNotFoundException.class)
    public String tableNotFoundExceptionHandler(TableNotFoundException exception){
        return exception.getMessage();
    }

@ExceptionHandler
    public String tableArleadyReservedException(TableArleadyReservedException exception){
        return exception.getMessage();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleOther(Exception ex) {
        return ResponseEntity.status(500).body("Internal server error");
    }

    @ExceptionHandler
    public String userNotFoundException (UserNotFoundException exception){
        return exception.getMessage();
    }
}
