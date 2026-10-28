package com.equadis.portal.modules.notification.infrastructure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on scheduled jobs; disabled with portal.notifications.enabled=false (the test profile does). */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "portal.notifications.enabled", havingValue = "true", matchIfMissing = true)
class SchedulingConfig {

}
