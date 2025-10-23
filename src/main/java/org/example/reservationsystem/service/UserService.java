package org.example.reservationsystem.service;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.DTO.UserProfileDTO;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(UserRepository userRepo, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepo = userRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    /** Liest Profildaten des Benutzers und mappt auf DTO. */
    @Transactional(readOnly = true)
    public UserProfileDTO getProfile(String username) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        UserProfileDTO dto = new UserProfileDTO();
        dto.setFullName(u.getFullName());
        dto.setEmail(u.getEmail());
        dto.setPhone(u.getPhone());
        return dto;
    }

    /** Aktualisiert Profildaten (vollständig oder teilweise – je pożądane). */
    @Transactional
    public void updateProfile(String username, UserProfileDTO dto) {
        User u = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Proste nadpisanie; jeśli chcesz partial update, dodaj null-checki
        if (dto.getFullName() != null) u.setFullName(dto.getFullName());
        if (dto.getEmail() != null)    u.setEmail(dto.getEmail());
        if (dto.getPhone() != null)    u.setPhone(dto.getPhone());

        userRepo.save(u);
    }

    /** Ändert das Passwort des Benutzers (prüft altes Passwort). */
    @Transactional
    public void changePassword(String username, ChangePasswordDTO changePasswordDTO) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!bCryptPasswordEncoder.matches(changePasswordDTO.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }
        user.setPassword(bCryptPasswordEncoder.encode(changePasswordDTO.getNewPassword()));
        userRepo.save(user);
    }
}