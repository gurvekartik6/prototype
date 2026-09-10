
        package in.jharkhand.civic.config;

import in.jharkhand.civic.security.DemoRoleFilter;
import in.jharkhand.civic.security.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain chain(
            HttpSecurity http,
            DemoRoleFilter demoRoleFilter,
            JwtAuthFilter jwt
    ) throws Exception {

        http
                .csrf(c -> c.disable())

                /*
                 * Enable CORS before authentication/security processing.
                 * This is required for browser preflight OPTIONS requests.
                 */
                .cors(c -> c.configurationSource(cors()))

                .sessionManagement(s ->
                        s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .exceptionHandling(e ->
                        e
                                .authenticationEntryPoint(unauthorized())
                                .accessDeniedHandler(forbidden())
                )

                .authorizeHttpRequests(a ->
                        a
                                /*
                                 * Public endpoints
                                 */
                                .requestMatchers(
                                        "/api/auth/**",
                                        "/api/health",
                                        "/actuator/health",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll()

                                /*
                                 * Browser CORS preflight requests must always
                                 * be allowed before authentication.
                                 */
                                .requestMatchers(HttpMethod.OPTIONS, "/**")
                                .permitAll()

                                /*
                                 * Everything else requires authentication.
                                 */
                                .anyRequest()
                                .authenticated()
                )

                /*
                 * Demo role authentication first.
                 */
                .addFilterBefore(
                        demoRoleFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                /*
                 * JWT authentication after demo-role authentication.
                 */
                .addFilterAfter(
                        jwt,
                        DemoRoleFilter.class
                );

        return http.build();
    }

    /**
     * Returns the JSON response for unauthenticated requests.
     */
    private AuthenticationEntryPoint unauthorized() {
        return (req, res, ex) -> {
            res.setStatus(401);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");

            res.getWriter().write(
                    "{\"status\":401,\"message\":\"Choose a demo role or sign in.\"}"
            );
        };
    }

    /**
     * Returns the JSON response when the authenticated user
     * does not have permission for the requested action.
     */
    private AccessDeniedHandler forbidden() {
        return (req, res, ex) -> {
            res.setStatus(403);
            res.setContentType("application/json");
            res.setCharacterEncoding("UTF-8");

            res.getWriter().write(
                    "{\"status\":403,\"message\":\"This action is not available for the selected role.\"}"
            );
        };
    }

    /**
     * CORS configuration for:
     *
     * Local development:
     *   http://localhost:5173
     *   http://localhost:3000
     *   http://127.0.0.1:5173
     *
     * Vercel:
     *   https://prototype-8ckq.vercel.app
     *
     * Preview deployments:
     *   https://*.vercel.app
     */
    private CorsConfigurationSource cors() {

        CorsConfiguration c = new CorsConfiguration();

        c.setAllowedOriginPatterns(
                List.of(
                        "https://prototype-8ckq.vercel.app",
                        "https://*.vercel.app",
                        "http://localhost:*",
                        "http://127.0.0.1:*"
                )
        );

        c.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * Your frontend sends:
         *
         * Authorization
         * X-Demo-Role
         * X-Demo-User-Id
         * Content-Type
         *
         * Wildcard allows all of these without needing
         * to maintain a manual list.
         */
        c.setAllowedHeaders(List.of("*"));

        c.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        /*
         * Your current Axios setup does not use cookies for
         * authentication, so credentials are not required.
         */
        c.setAllowCredentials(false);

        /*
         * Cache successful preflight responses for 1 hour.
         */
        c.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", c);

        return source;
    }
}

