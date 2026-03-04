package com.example.lms.config;

import com.example.lms.auth.service.CustomUserDetailsService;
import com.example.lms.enrollment.entity.LoginHistoryEntity;
import com.example.lms.enrollment.repo.LoginHistoryJpaRepository;
import com.example.lms.enrollment.repo.UserJpaRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;

import java.io.IOException;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final UserJpaRepository userJpaRepository;
    private final LoginHistoryJpaRepository loginHistoryJpaRepository;

    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          UserJpaRepository userJpaRepository,
                          LoginHistoryJpaRepository loginHistoryJpaRepository) {
        this.userDetailsService = userDetailsService;
        this.userJpaRepository = userJpaRepository;
        this.loginHistoryJpaRepository = loginHistoryJpaRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .userDetailsService(userDetailsService)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/homepage", "/login", "/signup", "/signup/**", "/forgot-password", "/reset-password", "/css/**", "/js/**", "/images/**").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/user/**", "/enroll/**", "/enrollments/**", "/api/me/**", "/api/settings/**", "/payments", "/settings")
                    .hasAnyRole("USER", "ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("loginId")
                .passwordParameter("password")
                .successHandler(this::loginSuccessHandler)
                .failureUrl("/login?error")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .userDetailsService(userDetailsService)
                .key("lms-remember-me-secret-key-change-this")
                .rememberMeParameter("remember-me")
                .tokenValiditySeconds(14 * 24 * 60 * 60)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "remember-me")
                .permitAll()
            );

        return http.build();
    }

    private void loginSuccessHandler(HttpServletRequest request,
                                     HttpServletResponse response,
                                     org.springframework.security.core.Authentication authentication)
            throws IOException, ServletException {

        String loginId = authentication.getName();
        HttpSession session = request.getSession(true);
        userJpaRepository.findByLoginId(loginId).ifPresent(u -> {
            session.setAttribute("loginUserId", u.getId());
            session.setAttribute("loginUserName", u.getName());
            session.setAttribute("loginUserEmail", loginId);
            session.setAttribute("loginUserRole", u.getRole());
        });

        String xff = request.getHeader("X-Forwarded-For");
        String ipAddress = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
        userJpaRepository.findByLoginId(loginId).ifPresent(u -> {
            LoginHistoryEntity h = new LoginHistoryEntity();
            h.setUserId(u.getId());
            h.setLoginTime(java.time.LocalDateTime.now());
            h.setIpAddress(ipAddress);
            h.setUserAgent(request.getHeader("User-Agent"));
            loginHistoryJpaRepository.save(h);
        });

        RequestCache requestCache = new HttpSessionRequestCache();
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String target = savedRequest.getRedirectUrl();
            if (target.endsWith("?continue")) target = target.replace("?continue", "");
            response.sendRedirect(target);
            return;
        }

        response.sendRedirect("/");
    }
}
