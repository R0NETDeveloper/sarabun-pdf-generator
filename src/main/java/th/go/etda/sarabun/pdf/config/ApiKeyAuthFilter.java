package th.go.etda.sarabun.pdf.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import th.go.etda.sarabun.pdf.model.ApiResponse;

/**
 * API Key Authentication Filter
 *
 * ตรวจสอบ X-API-Key header สำหรับ API endpoints
 *
 * Endpoints ที่ไม่ต้องตรวจสอบ:
 * - /actuator/** (health check)
 * - /swagger-ui/** (API docs)
 * - /api-docs/** (OpenAPI spec)
 * - /api-tester.html (dev tool)
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyAuthFilter.class);
    private static final String API_KEY_HEADER = "X-API-Key";

    private final String validApiKey;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(String validApiKey) {
        this.validApiKey = validApiKey;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Skip authentication for public endpoints
        if (isPublicEndpoint(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check API Key
        String providedApiKey = request.getHeader(API_KEY_HEADER);

        if (providedApiKey == null || providedApiKey.isBlank()) {
            log.warn("Missing API Key for request: {} {}", request.getMethod(), requestPath);
            sendUnauthorizedResponse(response, "Missing API Key. Please provide X-API-Key header.");
            return;
        }

        if (!validApiKey.equals(providedApiKey)) {
            log.warn("Invalid API Key for request: {} {} from IP: {}",
                    request.getMethod(), requestPath, getClientIp(request));
            sendUnauthorizedResponse(response, "Invalid API Key.");
            return;
        }

        // API Key is valid, continue
        filterChain.doFilter(request, response);
    }

    /**
     * ตรวจสอบว่าเป็น public endpoint หรือไม่
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/actuator") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/api-docs") ||
               path.startsWith("/v3/api-docs") ||
               path.equals("/api-tester.html") ||
               path.equals("/favicon.ico") ||
               path.startsWith("/webjars/");
    }

    /**
     * ส่ง 401 Unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> errorResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * ดึง Client IP
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
