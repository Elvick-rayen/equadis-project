package com.equadis.portal.modules.notification.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.equadis.portal.modules.notification.domain.ExpiryNotice;
import com.equadis.portal.modules.notification.domain.Notifier;

/** Notifier adapter that writes to the application log. */
@Component
class LoggingNotifier implements Notifier {

	private static final Logger log = LoggerFactory.getLogger(LoggingNotifier.class);

	@Override
	public void notify(ExpiryNotice notice) {
		log.info("[expiry notification] {}", notice.message());
	}

}
