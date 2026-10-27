package com.equadis.portal.shared.time;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One clock for the whole app, in the Europe/Paris zone the brief uses for all dates. Tests can replace it. */
@Configuration
public class ClockConfig {

	public static final ZoneId ZONE = ZoneId.of("Europe/Paris");

	@Bean
	Clock clock() {
		return Clock.system(ZONE);
	}

}
