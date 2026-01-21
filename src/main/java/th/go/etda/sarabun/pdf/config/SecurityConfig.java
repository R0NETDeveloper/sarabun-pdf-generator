package th.go.etda.sarabun.pdf.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration
 *
 * รองรับ 2 โหมด:
 * 1. Development (api.security.enabled=false): ไม่ต้องใช้ API Key
 * 2. Production (api.security.enabled=true): ต้องใช้ API Key
 *
 * การใช้งาน:
 * - localhost/dev: ตั้ง api.security.enabled=false
 * - production: ตั้ง api.security.enabled=true และกำหนด api.key
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${api.security.enabled:false}")
    private boolean securityEnabled;

    @Value("${api.key:}")
    private String apiKey;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ปิด CSRF สำหรับ REST API (ใช้ API Key แทน)
            .csrf(csrf -> csrf.disable())

            // ปิด session (stateless API)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // อนุญาต iframe สำหรับ Swagger UI
            .headers(headers -> headers
                .frameOptions(frame -> frame.disable())
            )

            // กำหนดสิทธิ์
            .authorizeHttpRequests(auth -> auth
                // Public endpoints - ไม่ต้อง auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/api-docs/**").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()
                .requestMatchers("/api-tester.html").permitAll()
                .requestMatchers("/webjars/**").permitAll()
                .requestMatchers("/favicon.ico").permitAll()
                // ที่เหลือ - ขึ้นอยู่กับ security mode
                .anyRequest().permitAll()
            );

        // เพิ่ม API Key filter ถ้าเปิด security
        if (securityEnabled) {
            if (apiKey == null || apiKey.isBlank()) {
                log.error("API Security is enabled but api.key is not configured!");
                throw new IllegalStateException(
                    "API Security is enabled but api.key is not configured. " +
                    "Please set 'api.key' in application.properties or environment variable."
                );
            }

            log.info("API Key authentication is ENABLED");
            log.info("API Key (first 8 chars): {}...", apiKey.substring(0, Math.min(8, apiKey.length())));

            http.addFilterBefore(
                new ApiKeyAuthFilter(apiKey),
                UsernamePasswordAuthenticationFilter.class
            );
        } else {
            log.warn("API Key authentication is DISABLED (development mode)");
            log.warn("Do NOT use this configuration in production!");
        }

        return http.build();
    }
}
