package org.example.reservationsystem.service;

import org.example.reservationsystem.DTO.ChangePasswordDTO;
import org.example.reservationsystem.DTO.UserDTO;
import org.example.reservationsystem.model.User;
import org.example.reservationsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private final UserRepository userRepo;

    @Autowired
    BCryptPasswordEncoder bCryptPasswordEncoder;

    public UserService(UserRepository userRepo,BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepo = userRepo;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepo.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }


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

