package com.example.lms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI lmsOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("LMS API")
                .description("LMS 프로젝트 API 문서")
                .version("v1.0.0")
                .contact(new Contact().name("LMS"))
                .license(new License().name("Internal Use")));
    }
}
