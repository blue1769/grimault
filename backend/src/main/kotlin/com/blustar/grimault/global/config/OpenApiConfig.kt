package com.blustar.grimault.global.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun openApi(): OpenAPI {
        return OpenAPI()
            .info(apiInfo())
    }

    private fun apiInfo(): Info {
        return Info()
            .title("Grimault")
            .description("The Sovereign Financial Ledger & Private Archive")
            .version("0.0.1")
    }
}
