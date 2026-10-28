package com.equadis.portal.modules.partnership.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.equadis.portal.modules.partnership.PartnershipStatus;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.DateRange;
import com.equadis.portal.shared.domain.ErrorCode;

/** Partnership rules on chosen days; no clock, no database. */
class PartnershipTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 3, 1);

	private static Partnership partnership(LocalDate start, LocalDate end) {
		return Partnership.create(1, 2, DateRange.of(start, end), start.isBefore(TODAY) ? start : TODAY);
	}

	private static void assertFails(Runnable action, ErrorCode code) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(code));
	}

	@Nested
	class Creation {

		@Test
		void startsTodayOrLaterAndLastsAtMost36Months() {
			Partnership p = Partnership.create(1, 2, DateRange.of(TODAY, TODAY.plusMonths(36)), TODAY);
			assertThat(p.startDate()).isEqualTo(TODAY);
		}

		@Test
		void endMustBeAfterStart() {
			assertFails(() -> DateRange.of(TODAY, TODAY), ErrorCode.INVALID_DATE_RANGE);
			assertFails(() -> DateRange.of(TODAY, TODAY.minusDays(1)), ErrorCode.INVALID_DATE_RANGE);
		}

		@Test
		void cannotStartInThePast() {
			assertFails(() -> Partnership.create(1, 2, DateRange.of(TODAY.minusDays(1), TODAY.plusMonths(1)), TODAY),
					ErrorCode.START_DATE_IN_PAST);
		}

		@Test
		void cannotLastMoreThan36Months() {
			assertFails(() -> Partnership.create(1, 2, DateRange.of(TODAY, TODAY.plusMonths(36).plusDays(1)), TODAY),
					ErrorCode.PARTNERSHIP_TOO_LONG);
		}

	}

	@Nested
	class Status {

		private final Partnership p = partnership(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));

		@Test
		void followsTheDatesWithoutAnyAction() {
			assertThat(p.status(LocalDate.of(2026, 3, 31))).isEqualTo(PartnershipStatus.SCHEDULED);
			assertThat(p.status(LocalDate.of(2026, 4, 1))).isEqualTo(PartnershipStatus.ACTIVE);
			assertThat(p.status(LocalDate.of(2026, 9, 30))).isEqualTo(PartnershipStatus.ACTIVE);
			assertThat(p.status(LocalDate.of(2026, 10, 1))).isEqualTo(PartnershipStatus.EXPIRED);
		}

		@Test
		void terminatedWinsOverDates() {
			p.terminate("Supplier stopped the product line", LocalDate.of(2026, 5, 1));
			assertThat(p.status(LocalDate.of(2026, 6, 1))).isEqualTo(PartnershipStatus.TERMINATED);
		}

	}

	@Nested
	class Renewal {

		private final Partnership p = partnership(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));

		@Test
		void anActivePartnershipCanBeRenewedAndKeepsItsHistory() {
			LocalDate day = LocalDate.of(2026, 6, 1);
			p.renew(LocalDate.of(2027, 3, 31), day);
			p.renew(LocalDate.of(2027, 9, 30), day);

			assertThat(p.endDate()).isEqualTo(LocalDate.of(2027, 9, 30));
			assertThat(p.renewals()).extracting(r -> r.previousEndDate(), r -> r.newEndDate())
				.containsExactly(org.assertj.core.groups.Tuple.tuple(LocalDate.of(2026, 9, 30), LocalDate.of(2027, 3, 31)),
						org.assertj.core.groups.Tuple.tuple(LocalDate.of(2027, 3, 31), LocalDate.of(2027, 9, 30)));
		}

		@Test
		void anExpiredPartnershipCanBeRenewedFor30DaysOnly() {
			LocalDate thirtyDaysAfterEnd = LocalDate.of(2026, 10, 30);
			assertThat(p.canRenew(thirtyDaysAfterEnd)).isTrue();
			assertThat(p.canRenew(thirtyDaysAfterEnd.plusDays(1))).isFalse();
			assertFails(() -> p.renew(LocalDate.of(2027, 6, 30), thirtyDaysAfterEnd.plusDays(1)),
					ErrorCode.RENEWAL_NOT_ALLOWED);

			p.renew(LocalDate.of(2027, 6, 30), thirtyDaysAfterEnd);
			assertThat(p.status(thirtyDaysAfterEnd)).isEqualTo(PartnershipStatus.ACTIVE);
		}

		@Test
		void scheduledOrTerminatedPartnershipsCannotBeRenewed() {
			assertThat(p.canRenew(LocalDate.of(2026, 3, 15))).isFalse();
			p.terminate("Reason", LocalDate.of(2026, 5, 1));
			assertFails(() -> p.renew(LocalDate.of(2027, 1, 1), LocalDate.of(2026, 5, 2)), ErrorCode.RENEWAL_NOT_ALLOWED);
		}

		@Test
		void theNewEndMustMoveForwardAndRespect36Months() {
			LocalDate day = LocalDate.of(2026, 6, 1);
			assertFails(() -> p.renew(LocalDate.of(2026, 9, 30), day), ErrorCode.INVALID_RENEWAL_DATE);
			assertFails(() -> p.renew(LocalDate.of(2026, 9, 30).plusMonths(36).plusDays(1), day),
					ErrorCode.PARTNERSHIP_TOO_LONG);
		}

	}

	@Nested
	class Termination {

		@Test
		void scheduledAndActivePartnershipsCanBeTerminatedWithAReason() {
			Partnership scheduled = partnership(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));
			scheduled.terminate("  Not needed anymore  ", TODAY);
			assertThat(scheduled.terminationReason()).isEqualTo("Not needed anymore");
			assertThat(scheduled.terminatedOn()).isEqualTo(TODAY);
		}

		@Test
		void theReasonIsMandatory() {
			Partnership p = partnership(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));
			assertFails(() -> p.terminate("  ", TODAY), ErrorCode.TERMINATION_REASON_REQUIRED);
		}

		@Test
		void expiredOrTerminatedPartnershipsCannotBeTerminated() {
			Partnership p = partnership(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));
			assertFails(() -> p.terminate("Too late", LocalDate.of(2026, 10, 1)), ErrorCode.TERMINATION_NOT_ALLOWED);
			p.terminate("First", LocalDate.of(2026, 5, 1));
			assertFails(() -> p.terminate("Second", LocalDate.of(2026, 5, 2)), ErrorCode.TERMINATION_NOT_ALLOWED);
		}

		@Test
		void allowedActionsMatchTheRules() {
			Partnership p = partnership(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 9, 30));
			assertThat(p.allowedActions(LocalDate.of(2026, 3, 15))).containsExactly(PartnershipAction.TERMINATE);
			assertThat(p.allowedActions(LocalDate.of(2026, 6, 1))).containsExactly(PartnershipAction.RENEW,
					PartnershipAction.TERMINATE);
			assertThat(p.allowedActions(LocalDate.of(2026, 10, 10))).containsExactly(PartnershipAction.RENEW);
			assertThat(p.allowedActions(LocalDate.of(2026, 12, 1))).isEmpty();
		}

	}

}
