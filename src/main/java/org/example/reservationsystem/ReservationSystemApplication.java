package org.example.reservationsystem;

import org.example.reservationsystem.model.RestaurantTable;
import org.example.reservationsystem.model.Role;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.TableRepository;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class ReservationSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReservationSystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(UserRepository userRepo,
                                      TableRepository tableRepo,
                                      BCryptPasswordEncoder encoder) {
        return args -> {

            // Tworzymy konto admina jeśli nie istnieje
            if (userRepo.findByUsername("admin").isEmpty()) {
                String encodedPassword = encoder.encode("admin123");
                User admin = new User("admin", encodedPassword, Role.ROLE_ADMIN);
                userRepo.save(admin);
                System.out.println("✅ Admin user created.");
            }

            // Tworzymy zestaw stolików (numer + liczba miejsc)
            int[][] tables = {
                    {1, 2}, {2, 3}, {3, 4},
                    {4, 6}, {5, 2}, {6, 8}
            };

            for (int[] t : tables) {
                int tableNumber = t[0];
                int seats = t[1];
                if (tableRepo.findTableByTableNumber(tableNumber).isEmpty()) {
                    RestaurantTable table = new RestaurantTable(seats, tableNumber);
                    tableRepo.save(table);
                    System.out.println("🪑 Added table " + tableNumber + " (" + seats + " seats)");
                }
            }
        };
    }
}