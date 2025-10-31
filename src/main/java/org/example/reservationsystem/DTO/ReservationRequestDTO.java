package org.example.reservationsystem.DTO;



import java.time.LocalDateTime;

public class ReservationRequestDTO {
    private int tableNumber;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public int getTableNumber() { return tableNumber; }
    public void setTableNumber(int tableNumber) { this.tableNumber = tableNumber; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}