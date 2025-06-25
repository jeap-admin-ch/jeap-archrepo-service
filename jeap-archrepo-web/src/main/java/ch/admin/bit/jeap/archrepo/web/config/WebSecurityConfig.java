package ch.admin.bit.jeap.archrepo.web.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
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
        RequestMatcher apiExceptDatabaseSchema = new AndRequestMatcher(
                new AntPathRequestMatcher("/api/**"), // basic auth protected or unprotected
                new NegatedRequestMatcher(new AntPathRequestMatcher("/api/dbschemas/**")) // OAuth2 protected
        );
        http.securityMatcher(apiExceptDatabaseSchema);
        http.authorizeHttpRequests(r -> r
                .requestMatchers(HttpMethod.GET, "/api/model").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/model/*/relations").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/model/rest-api-relation-without-pact").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/model/system-components-without-open-api-spec").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/openapi/**").permitAll()
                .anyRequest().hasRole("api"));
        http.csrf(AbstractHttpConfigurer::disable);
        http.httpBasic(withDefaults());
        http.authenticationManager(createApiAuthManager(http.getSharedObject(AuthenticationManagerBuilder.class)));
        return http.build();
    }
    //@formatter:on

    private AuthenticationManager createApiAuthManager(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().
                withUser("api").
                password(apiSecret).
                roles("api");
        return auth.build();
    }
}
