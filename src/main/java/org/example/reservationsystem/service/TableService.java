package org.example.reservationsystem.service;

import org.example.reservationsystem.model.RestaurantTable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.example.reservationsystem.repository.TableRepository;

@Service
public class TableService {

private final TableRepository tableRepository;

@Autowired
    public TableService(TableRepository tableRepository) {
    this.tableRepository = tableRepository;
    }


    public RestaurantTable getTable(Long tableId){
        return tableRepository.findById(tableId).orElse(null);
    }

    public RestaurantTable addTable(RestaurantTable restaurantTable){
    return tableRepository.save(restaurantTable);
    }

    public void deleteTable(Long tableId){
    tableRepository.deleteById(tableId);
    }
}
