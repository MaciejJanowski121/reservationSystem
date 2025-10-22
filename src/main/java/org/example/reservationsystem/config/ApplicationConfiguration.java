package org.example.reservationsystem.config;

import org.example.reservationsystem.JWTServices.JwtAuthenticationFilter;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class ApplicationConfiguration {

    private final UserRepository userRepository;

    public ApplicationConfiguration(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Konfiguration des benutzerdefinierten UserDetailsService, der Benutzer aus der Datenbank lädt
    @Bean
    public UserDetailsService userDetailsService() {
        return username -> userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    // Password-Encoder mit BCrypt (wird z. B. für die Registrierung verwendet)
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Konfiguration des AuthenticationManagers mit dem benutzerdefinierten AuthenticationProvider
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        return new ProviderManager(authenticationProvider);
    }

    // Konfiguration des DaoAuthenticationProviders mit UserDetailsService und Passwort-Encoder
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Hauptkonfiguration der HTTP-Sicherheitsfilterkette
    @Bean
    public SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {

        return http
                // CORS aktivieren (wird z. B. für Frontend-Anfragen benötigt)
                .cors(cors -> cors.configure(http))

                // CSRF deaktivieren (nicht erforderlich bei stateless JWT-Authentifizierung)
                .csrf(csrf -> csrf.disable())

                // Sitzung deaktivieren – stateless, da JWT verwendet wird
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Zugriffskontrollen konfigurieren
                .authorizeHttpRequests(auth -> {
                    // OPTIONS-Anfragen immer erlauben (CORS Preflight)
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // Endpunkte für Registrierung und Login offen lassen
                    auth.requestMatchers("/auth/register").permitAll();
                    auth.requestMatchers("/auth/login").permitAll();
                    auth.requestMatchers("/auth/auth_check").permitAll();

                    // Authentifizierung erforderlich für Benutzer-Reservierungs-Endpunkte
                    auth.requestMatchers(HttpMethod.POST, "/api/reservations").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/api/reservations/userReservations").authenticated();
                    auth.requestMatchers(HttpMethod.DELETE, "/api/reservations/**").authenticated();

                    // Nur Admins dürfen alle Reservierungen sehen
                    auth.requestMatchers("/api/reservations/all").hasAuthority("ROLE_ADMIN");

                    // Adminpanel nur für ROLE_ADMIN
                    auth.requestMatchers("/admin/**").hasAuthority("ROLE_ADMIN");

                    // Alle anderen Anfragen erfordern Authentifizierung
                    auth.anyRequest().authenticated();
                })

                // Fehlerbehandlung bei fehlender Authentifizierung
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(401, "Unauthorized");
                        })
                )

                // JWT-Filter vor dem UsernamePasswordAuthenticationFilter einfügen
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // Sicherheitskette abschließen
                .build();
    }
}