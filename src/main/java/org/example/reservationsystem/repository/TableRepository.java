package org.example.reservationsystem.repository;

import org.example.reservationsystem.model.RestaurantTable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TableRepository extends JpaRepository<RestaurantTable, Long> {

    Optional<RestaurantTable> findTableByTableNumber(int tableNumber);
}
