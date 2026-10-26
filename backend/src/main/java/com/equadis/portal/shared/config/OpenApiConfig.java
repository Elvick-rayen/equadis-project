package com.equadis.portal.shared.config;

import java.util.Arrays;

import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.equadis.portal.shared.actor.CurrentActor;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;

@Configuration
public class OpenApiConfig {

	@Bean
	OpenAPI portalOpenApi() {
		return new OpenAPI().info(new Info().title("Equadis Partnership Portal API")
			.version("v1")
			.description("Suppliers, retailers, partnerships and offers. "
					+ "Every call states its persona in the " + CurrentActor.HEADER + " header."));
	}

	/** Documents the X-Acting-As header on every operation that takes a CurrentActor. */
	@Bean
	OperationCustomizer actingAsHeader() {
		return (operation, handlerMethod) -> {
			boolean usesActor = Arrays.stream(handlerMethod.getMethodParameters())
				.anyMatch(parameter -> parameter.getParameterType() == CurrentActor.class);
			if (usesActor) {
				operation.addParametersItem(new HeaderParameter().name(CurrentActor.HEADER)
					.required(true)
					.description("Persona: ADMIN, SUPPLIER:<id> or RETAILER:<id>")
					.example("ADMIN")
					.schema(new StringSchema()));
			}
			return operation;
		};
	}

}
