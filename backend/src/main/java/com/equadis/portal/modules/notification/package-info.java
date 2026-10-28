/**
 * Expiry notifications (bonus): a daily job, at 08:00 Europe/Paris, announces partnerships that
 * end in exactly 30, 7 or 1 days, through the {@link com.equadis.portal.modules.notification.domain.Notifier} port.
 */
@ApplicationModule(displayName = "Notifications")
package com.equadis.portal.modules.notification;

import org.springframework.modulith.ApplicationModule;
