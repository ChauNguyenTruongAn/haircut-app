package com.haircutbooking.Haircut_Booking.services;

import java.util.List;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.haircutbooking.Haircut_Booking.domain.User;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.ChangePasswordRequest;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.ModifyUserRequest;
import com.haircutbooking.Haircut_Booking.domain.RequestDTO.RegisterRequest;
import com.haircutbooking.Haircut_Booking.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;

    public List<User> getAllUser() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) throws Exception {
        return userRepository.findById(id).orElseThrow(() -> new Exception("Not found user"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().getRoleName())
                .build();
    }

    public User addUser(RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .phoneNumber(request.getSdt())
                .email(request.getEmail())
                .fullName(request.getHoTen())
                .build();
        return userRepository.save(user);
    }

    public User modifyUser(Long id, ModifyUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user"));
        user.setEmail(request.getEmail());
        user.setPhoneNumber(request.getSdt());
        user.setFullName(request.getHoTen());
        return userRepository.save(user);
    }

    public User changePasswordUser(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Not found user"));
        user.setPassword(request.getPassword());
        return userRepository.save(user);
    }
}
