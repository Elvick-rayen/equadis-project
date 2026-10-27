package com.equadis.portal.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {

	private static final String BEARER = "bearerAuth";

	@Bean
	OpenAPI portalOpenApi() {
		return new OpenAPI()
			.info(new Info().title("Equadis Partnership Portal API")
				.version("v1")
				.description("Suppliers, retailers, partnerships and offers. "
						+ "Sign in with POST /api/auth/login, then click Authorize and paste the token."))
			.components(new Components().addSecuritySchemes(BEARER,
					new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
			.addSecurityItem(new SecurityRequirement().addList(BEARER));
	}

}
