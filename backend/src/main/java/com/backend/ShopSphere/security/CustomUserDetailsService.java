package com.backend.ShopSphere.security;

import com.backend.ShopSphere.entity.User;
import com.backend.ShopSphere.exception.UnauthorizedException;
import com.backend.ShopSphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email){
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new UsernameNotFoundException("User not found")
        );

        return org.springframework.security.core.userdetails.User
                .builder()
                .username(
                        user.getEmail()
                )
                .password(
                        user.getPassword()
                )
                .roles(
                        user.getRole().name()
                )
                .build();

    }
}
