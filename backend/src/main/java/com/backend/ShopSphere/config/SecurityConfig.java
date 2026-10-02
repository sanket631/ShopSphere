package com.backend.ShopSphere.config;

import com.backend.ShopSphere.security.CustomAccessDeniedHandler;
import com.backend.ShopSphere.security.CustomUserDetailsService;
import com.backend.ShopSphere.security.JwtAuthenticationEntryPoint;
import com.backend.ShopSphere.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final JwtAuthenticationEntryPoint entryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> {})
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // Public
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/product-variants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/**").permitAll()

                        // Products - Seller/Admin
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("SELLER", "ADMIN")

                        // Product Variants - Seller/Admin
                        .requestMatchers(HttpMethod.POST, "/api/product-variants/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/product-variants/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/product-variants/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/product-variants/**").hasAnyRole("SELLER", "ADMIN")

                        // Categories - Seller/Admin
                        .requestMatchers(HttpMethod.POST, "/api/categories/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/categories/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/categories/**").hasAnyRole("SELLER", "ADMIN")

                        // Cart - Customer/Admin
                        .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Address - Customer/Admin
                        .requestMatchers("/api/address/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Orders - Customer/Admin (Seller can view)
                        .requestMatchers(HttpMethod.POST, "/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/orders/**").hasAnyRole("CUSTOMER", "SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/orders/**").hasRole("ADMIN")

                        // Order Items - Customer/Seller/Admin
                        .requestMatchers(HttpMethod.GET, "/api/order-items/**").hasAnyRole("CUSTOMER", "SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/order-items/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/order-items/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/order-items/**").hasAnyRole("CUSTOMER", "SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/order-items/**").hasRole("ADMIN")

                        // Payments - Customer/Admin
                        .requestMatchers("/api/payments/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Coupons - Apply for Customer, Manage for Seller/Admin
                        .requestMatchers(HttpMethod.POST, "/api/coupons/apply").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/coupons/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/coupons/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/coupons/**").hasAnyRole("SELLER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/coupons/**").hasAnyRole("SELLER", "ADMIN")

                        // Wishlist - Customer/Admin
                        .requestMatchers("/api/wishlist/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Reviews - Customer/Admin
                        .requestMatchers(HttpMethod.POST, "/api/reviews/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/reviews/**").hasAnyRole("CUSTOMER", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/reviews/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Shopping - Customer/Admin
                        .requestMatchers("/api/shopping/**").hasAnyRole("CUSTOMER", "ADMIN")

                        // Seller endpoints
                        .requestMatchers("/api/seller/**").hasAnyRole("SELLER", "ADMIN")

                        // Shipments - Seller/Admin (TODO: add Customer read access later)
                        .requestMatchers("/api/shipments/**").hasAnyRole("SELLER", "ADMIN")

                        // Admin
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        .anyRequest()
                        .authenticated()

                ).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(
                        exception -> exception
                                .authenticationEntryPoint(
                                        entryPoint
                                )
                                .accessDeniedHandler(
                                        accessDeniedHandler
                                )
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception{
        return config.getAuthenticationManager();
    }
}

