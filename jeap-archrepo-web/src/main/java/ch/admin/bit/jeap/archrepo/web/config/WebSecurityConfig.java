package ch.admin.bit.jeap.archrepo.web.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
public class WebSecurityConfig {

    @Value("${archrepo.api.secret}")
    private String apiSecret;

    //@formatter:off
    @Bean
    @Order(100) // same as on the deprecated WebSecurityConfigurerAdapter
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        PathPatternRequestMatcher.Builder api = PathPatternRequestMatcher.withDefaults().basePath("/api");
        RequestMatcher apiExcludedDatabaseSchemaExcludedOpenApiPostWithBearerAuth = new AndRequestMatcher(
                api.matcher("/**"),
                new NegatedRequestMatcher(api.matcher("/dbschemas/**")), // OAuth2 protected
                new NegatedRequestMatcher(postToOpenApiWithBearerTokenRequestMatcher(api)) // OAuth2 protected
        );
        http.securityMatcher(apiExcludedDatabaseSchemaExcludedOpenApiPostWithBearerAuth);
        http.authorizeHttpRequests(r -> r
                .requestMatchers(HttpMethod.GET, "/api/model").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/model/*/relations").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/model/rest-api-relation-without-pact").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/model/system-components-without-open-api-spec").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/openapi/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reactions/**").permitAll()
                .anyRequest().hasRole("api"));
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(withDefaults());
        http.authenticationManager(createApiAuthManager(http.getSharedObject(AuthenticationManagerBuilder.class)));
        return http.build();
    }
    //@formatter:on

    private RequestMatcher postToOpenApiWithBearerTokenRequestMatcher(PathPatternRequestMatcher.Builder api) {
        return new AndRequestMatcher(api.matcher("/openapi/**"), this::isPostWithBearerAuth);
    }

    private boolean isPostWithBearerAuth(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        boolean isBearerAuth = authHeader != null && authHeader.startsWith("Bearer ");
        boolean isPost = HttpMethod.POST.matches(request.getMethod());
        return isPost && isBearerAuth;
    }

    private AuthenticationManager createApiAuthManager(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().
                withUser("api").
                password(apiSecret).
                roles("api");
        return auth.build();
    }
}
