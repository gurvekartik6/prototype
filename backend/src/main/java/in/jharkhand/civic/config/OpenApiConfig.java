package in.jharkhand.civic.config;
import io.swagger.v3.oas.models.*; import io.swagger.v3.oas.models.info.Info; import org.springframework.context.annotation.*;
@Configuration public class OpenApiConfig { @Bean OpenAPI api(){return new OpenAPI().info(new Info().title("Jharkhand Civic Innovation Platform API").version("1.0.0").description("SIH-ready civic problem resolution platform API."));} }
