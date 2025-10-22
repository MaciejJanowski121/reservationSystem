package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

public Reservation findById(long id);

List<Reservation> findReservationsByName(String name);
}
