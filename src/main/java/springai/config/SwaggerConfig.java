package springai.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("智能总结pdf助手")
                        .version("1.0")
                        .description("API描述信息"))
                .externalDocs(new ExternalDocumentation()
                        .description("完整文档")
                        .url("https://doc.example.com"));
    }
}