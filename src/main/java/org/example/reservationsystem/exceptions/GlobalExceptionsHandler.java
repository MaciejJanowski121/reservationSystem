package org.example.reservationsystem.exceptions;

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

    @ExceptionHandler
    public String userNotFoundException (UserNotFoundException exception){
        return exception.getMessage();
    }
}
