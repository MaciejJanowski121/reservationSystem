package org.example.reservationsystem;

import org.example.reservationsystem.DTO.UserRegisterDTO;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldRegisterLoginAndCheckAuthSuccessfully() throws Exception {
        // Einzigartiger Benutzername, um Konflikte in der Testdatenbank zu vermeiden
        String username = "maciej_" + System.currentTimeMillis();

        // DTO über leeren Konstruktor und Setter erstellen
        UserRegisterDTO userDTO = new UserRegisterDTO();
        userDTO.setUsername(username);
        userDTO.setPassword("test123");
        userDTO.setFullName("Maciej Janowski");
        userDTO.setEmail(username + "@example.com");
        userDTO.setPhone("+49 170 0000000");

        // --- Registrierung ---
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.username").value(username));

        // --- Login ---
        var loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDTO)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andReturn();

        // Token-Wert aus dem Set-Cookie-Header extrahieren
        String setCookieHeader = loginResult.getResponse().getHeader("Set-Cookie");
        String tokenValue = setCookieHeader.split("token=")[1].split(";")[0];

        // --- Authentifizierung prüfen ---
        mockMvc.perform(get("/auth/auth_check")
                        .cookie(new MockCookie("token", tokenValue)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }
}