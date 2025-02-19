package ch.admin.bit.jeap.archrepo.web.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(title = "Architecture Repository Service API",
                description = "Architecture Repository Service API",
                version = "v1"
        ),
        security = @SecurityRequirement(name = "basicAuth")
)
@SecurityScheme(
        name = "basicAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "basic"
)
@ApiResponse(responseCode = "403", description = "Missing basic auth credentials")
@Configuration
public class OpenApiConfig {
    @Bean
    GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("Architecture-Repository-API")
                .pathsToMatch("/api/**")
                .packagesToScan("ch.admin.bit.jeap.archrepo.web.rest")
                .build();
    }

}
