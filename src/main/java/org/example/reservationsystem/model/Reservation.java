package org.example.reservationsystem.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import jakarta.persistence.*;

import java.time.LocalDateTime;
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;

    // Początek i koniec rezerwacji (żeby sprawdzać konflikty czasowe)
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // wiele rezerwacji może należeć do jednego stolika
    @ManyToOne
    @JoinColumn(name = "table_id")

    private RestaurantTable table;

    // jeden użytkownik może mieć tylko jedną aktywną rezerwację
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    public Reservation() {
    }

    public Reservation(String name, String email, String phone, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // --- Gettery i Settery ---
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }


    /// /

    public void setStartTime(LocalDateTime startTime) {
        if (startTime != null && startTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Reservation start time cannot be in the past.");
        }
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        if (endTime != null && startTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("End time cannot be before start time.");
        }
        this.endTime = endTime;
    }

    public RestaurantTable getTable() {
        return table;
    }

    public void setTable(RestaurantTable table) {
        this.table = table;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    // --- equals / hashCode ---
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Reservation that = (Reservation) obj;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}