package com.equadis.portal.modules.notification.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.equadis.portal.modules.notification.domain.ExpiryNotice;
import com.equadis.portal.modules.notification.domain.Notifier;
import com.equadis.portal.modules.partnership.PartnershipQueries;
import com.equadis.portal.modules.partnership.PartnershipQueries.ExpiringPartnership;
import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;

/**
 * Bonus: every day at 08:00 Europe/Paris, notifies about partnerships that end in exactly 30, 7
 * or 1 days. "Exactly" means each partnership is announced once per milestone, without keeping
 * track of what was already sent.
 */
@Component
public class ExpiryNotificationJob {

	/** Days before the end date at which a notice is sent. */
	public static final List<Integer> MILESTONES = List.of(30, 7, 1);

	private static final Logger log = LoggerFactory.getLogger(ExpiryNotificationJob.class);

	private final PartnershipQueries partnerships;

	private final SupplierQueries suppliers;

	private final RetailerQueries retailers;

	private final Notifier notifier;

	private final Clock clock;

	ExpiryNotificationJob(PartnershipQueries partnerships, SupplierQueries suppliers, RetailerQueries retailers,
			Notifier notifier, Clock clock) {
		this.partnerships = partnerships;
		this.suppliers = suppliers;
		this.retailers = retailers;
		this.notifier = notifier;
		this.clock = clock;
	}

	@Scheduled(cron = "${portal.notifications.expiry-cron:0 0 8 * * *}", zone = "Europe/Paris")
	public void runDaily() {
		int sent = notifyExpiringPartnerships();
		log.info("Expiry notification run: {} notice(s) sent", sent);
	}

	/** @return the number of notices sent */
	public int notifyExpiringPartnerships() {
		LocalDate today = LocalDate.now(clock);
		int sent = 0;
		for (int daysLeft : MILESTONES) {
			for (ExpiringPartnership partnership : partnerships.endingOn(today.plusDays(daysLeft))) {
				notifier.notify(new ExpiryNotice(partnership.partnershipId(),
						suppliers.findSummary(partnership.supplierId()).map(SupplierSummary::companyName).orElse("?"),
						retailers.findSummary(partnership.retailerId()).map(RetailerSummary::storeName).orElse("?"),
						partnership.endDate(), daysLeft));
				sent++;
			}
		}
		return sent;
	}

}
