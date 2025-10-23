package org.example.reservationsystem.DTO;

public  class TableViewDTO {
    private final Long id;
    private final Integer tableNumber;
    private final Integer numberOfSeats;

    public TableViewDTO(Long id, Integer tableNumber, Integer numberOfSeats) {
        this.id = id;
        this.tableNumber = tableNumber;
        this.numberOfSeats = numberOfSeats;
    }

    public Long getId() { return id; }
    public Integer getTableNumber() { return tableNumber; }
    public Integer getNumberOfSeats() { return numberOfSeats; }
}