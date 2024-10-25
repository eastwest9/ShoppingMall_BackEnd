package com.mealKit.backend.security;

import com.mealKit.backend.jwt.JwtFilter;
import com.mealKit.backend.jwt.JwtUtil;
import com.mealKit.backend.oauth2.CustomOAuth2UserService;
import com.mealKit.backend.oauth2.CustomSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final JwtUtil jwtUtil;

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{

        // csrf disable
        http
                .csrf((auth) -> auth.disable());

        // From 로그인 방식 disable
        http
                .formLogin((auth) -> auth.disable());

        // HTTP Basic 인증 방식 disable
        http
                .httpBasic((auth) -> auth.disable());

        // 세션 설정 : stateless
        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //JWTFilter 추가
        http
                .addFilterAfter(new JwtFilter(jwtUtil), OAuth2LoginAuthenticationFilter.class);


        // OAuth2
        http
                .oauth2Login((oauth2) -> oauth2
                        .successHandler(customSuccessHandler)
                        .loginProcessingUrl("/login")
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(customOAuth2UserService))
                );

        // 경로별 인가 작업
        http
                .authorizeHttpRequests((auth) -> auth
                    .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/login/**").permitAll()
                        .anyRequest().authenticated());


        // CORS 설정
        http
                .cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration configuration = new CorsConfiguration();

                        configuration.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
                        configuration.setAllowedMethods(Collections.singletonList("*"));
                        configuration.setAllowCredentials(true);
                        configuration.setAllowedHeaders(Collections.singletonList("*"));
                        configuration.setMaxAge(3600L);

                        configuration.setExposedHeaders(Collections.singletonList("Set-Cookie"));
                        configuration.setExposedHeaders(Collections.singletonList("Authorization"));
                        return configuration;
                    }
                }));


        return http.build();
    }
}