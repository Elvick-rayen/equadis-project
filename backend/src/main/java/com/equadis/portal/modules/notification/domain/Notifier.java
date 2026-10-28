package com.equadis.portal.modules.notification.domain;

/**
 * Port for sending notifications. The current adapter only logs; an e-mail or Slack adapter can
 * replace it without changing the job.
 */
public interface Notifier {

	void notify(ExpiryNotice notice);

}
