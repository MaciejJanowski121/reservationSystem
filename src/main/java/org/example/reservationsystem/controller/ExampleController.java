package org.example.reservationsystem.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExampleController {

    @GetMapping("/example")
    public String getExample() {
        return "CORS działa poprawnie!";
    }
}