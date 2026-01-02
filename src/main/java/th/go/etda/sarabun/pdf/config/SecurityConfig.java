package th.go.etda.sarabun.pdf.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security Configuration
 * 
 * ปิด authentication สำหรับ development
 * ในการใช้งานจริงควรเปิดใช้งาน JWT authentication
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // ปิด CSRF สำหรับ REST API
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable()) // อนุญาตให้แสดงใน iframe
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // อนุญาตทุก request (สำหรับ development)
            );
        
        return http.build();
    }
}
