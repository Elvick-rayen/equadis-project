package com.equadis.portal.shared.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.equadis.portal.shared.actor.CurrentActorArgumentResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final List<String> allowedOrigins;

	public WebConfig(@Value("${portal.cors.allowed-origins}") List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
		resolvers.add(new CurrentActorArgumentResolver());
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
			.allowedOrigins(allowedOrigins.toArray(String[]::new))
			.allowedMethods("GET", "POST", "PUT", "DELETE")
			.allowedHeaders(HttpHeaders.CONTENT_TYPE, HttpHeaders.AUTHORIZATION)
			.exposedHeaders(HttpHeaders.LOCATION);
	}

}
