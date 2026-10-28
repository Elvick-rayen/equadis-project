package com.equadis.portal.modules.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.equadis.portal.modules.notification.domain.ExpiryNotice;
import com.equadis.portal.modules.partnership.PartnershipQueries;
import com.equadis.portal.modules.partnership.PartnershipQueries.ExpiringPartnership;
import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;

class ExpiryNotificationJobTest {

	private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);

	@Test
	void notifiesPartnershipsEndingIn30_7And1DaysOnly() {
		PartnershipQueries partnerships = mock(PartnershipQueries.class);
		when(partnerships.endingOn(any())).thenReturn(List.of());
		when(partnerships.endingOn(TODAY.plusDays(30))).thenReturn(List.of(new ExpiringPartnership(1, 10, 20, TODAY.plusDays(30))));
		when(partnerships.endingOn(TODAY.plusDays(7))).thenReturn(List.of(new ExpiringPartnership(2, 10, 20, TODAY.plusDays(7))));
		when(partnerships.endingOn(TODAY.plusDays(1))).thenReturn(List.of(new ExpiringPartnership(3, 10, 20, TODAY.plusDays(1))));
		when(partnerships.endingOn(TODAY.plusDays(8))).thenReturn(List.of(new ExpiringPartnership(4, 10, 20, TODAY.plusDays(8))));

		SupplierQueries suppliers = mock(SupplierQueries.class);
		when(suppliers.findSummary(anyLong())).thenReturn(Optional.of(new SupplierSummary(10L, "Alpina Foods")));
		RetailerQueries retailers = mock(RetailerQueries.class);
		when(retailers.findSummary(anyLong())).thenReturn(Optional.of(new RetailerSummary(20L, "City Market Lyon")));

		List<ExpiryNotice> sent = new ArrayList<>();
		Clock clock = Clock.fixed(TODAY.atTime(8, 0).atZone(PARIS).toInstant(), PARIS);
		ExpiryNotificationJob job = new ExpiryNotificationJob(partnerships, suppliers, retailers, sent::add, clock);

		assertThat(job.notifyExpiringPartnerships()).isEqualTo(3);
		assertThat(sent).extracting(ExpiryNotice::partnershipId, ExpiryNotice::daysLeft)
			.containsExactly(org.assertj.core.groups.Tuple.tuple(1L, 30), org.assertj.core.groups.Tuple.tuple(2L, 7),
					org.assertj.core.groups.Tuple.tuple(3L, 1));
		assertThat(sent.get(2).message()).isEqualTo(
				"Partnership #3 between Alpina Foods and City Market Lyon ends in 1 day (2026-11-02)");
	}

}
