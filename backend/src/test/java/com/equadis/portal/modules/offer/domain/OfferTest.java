package com.equadis.portal.modules.offer.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.equadis.portal.modules.offer.DiscountType;
import com.equadis.portal.modules.offer.OfferStatus;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/** BR-08 (lifecycle) and BR-09 (discount and validity rules). */
class OfferTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 12, 1);

	private static OfferDetails details(Discount discount) {
		return new OfferDetails("ALP-WINTER-01", "Winter cereal promotion", null, discount, null, TODAY, TODAY.plusDays(30));
	}

	private static Offer draft() {
		return Offer.draft(1, details(Discount.of(DiscountType.PERCENTAGE_DISCOUNT, new BigDecimal("12"), null)));
	}

	private static void assertFails(Runnable action, ErrorCode code) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(code));
	}

	@Test
	void lifecycleOnlyMovesForward() {
		Offer offer = draft();
		assertThat(offer.status()).isEqualTo(OfferStatus.DRAFT);

		offer.publish(TODAY);
		assertThat(offer.status()).isEqualTo(OfferStatus.PUBLISHED);
		assertFails(() -> offer.publish(TODAY), ErrorCode.INVALID_OFFER_TRANSITION);

		offer.close();
		assertThat(offer.status()).isEqualTo(OfferStatus.CLOSED);
		assertFails(offer::close, ErrorCode.INVALID_OFFER_TRANSITION);
	}

	@Test
	void onlyDraftsCanBeEdited() {
		Offer offer = draft();
		offer.edit(details(Discount.of(DiscountType.PERCENTAGE_DISCOUNT, new BigDecimal("15"), null)));
		assertThat(offer.discount().value()).isEqualByComparingTo("15");

		offer.publish(TODAY);
		assertFails(() -> offer.edit(details(Discount.of(DiscountType.PERCENTAGE_DISCOUNT, BigDecimal.TEN, null))),
				ErrorCode.OFFER_NOT_EDITABLE);
	}

	@Test
	void draftsCannotBeClosedAndExpiredDraftsCannotBePublished() {
		assertFails(draft()::close, ErrorCode.INVALID_OFFER_TRANSITION);
		assertFails(() -> draft().publish(TODAY.plusDays(31)), ErrorCode.INVALID_OFFER_TRANSITION);
	}

	@Test
	void openOnlyWhenPublishedAndWithinTheValidityWindow() {
		Offer offer = draft();
		assertThat(offer.isOpenOn(TODAY)).isFalse();
		offer.publish(TODAY);
		assertThat(offer.isOpenOn(TODAY)).isTrue();
		assertThat(offer.isOpenOn(TODAY.plusDays(30))).isTrue();
		assertThat(offer.isOpenOn(TODAY.plusDays(31))).isFalse();
		assertThat(offer.isOpenOn(TODAY.minusDays(1))).isFalse();
	}

	@ParameterizedTest(name = "{0} {1} {2} -> valid={3}")
	@CsvSource(nullValues = "-", value = {
			"PERCENTAGE_DISCOUNT,    0.01,   -,   true",
			"PERCENTAGE_DISCOUNT,    100,    -,   true",
			"PERCENTAGE_DISCOUNT,    0,      -,   false",
			"PERCENTAGE_DISCOUNT,    100.01, -,   false",
			"PERCENTAGE_DISCOUNT,    10,     EUR, false",
			"FIXED_AMOUNT_DISCOUNT,  250,    EUR, true",
			"FIXED_AMOUNT_DISCOUNT,  250,    eur, true",
			"FIXED_AMOUNT_DISCOUNT,  0,      EUR, false",
			"FIXED_AMOUNT_DISCOUNT,  -5,     EUR, false",
			"FIXED_AMOUNT_DISCOUNT,  250,    -,   false",
			"FIXED_AMOUNT_DISCOUNT,  250,    XYZ, false",
			"FIXED_AMOUNT_DISCOUNT,  1.005,  EUR, false" })
	void discountRules(DiscountType type, BigDecimal value, String currency, boolean valid) {
		if (valid) {
			assertThat(Discount.of(type, value, currency).type()).isEqualTo(type);
		}
		else {
			assertFails(() -> Discount.of(type, value, currency), ErrorCode.INVALID_DISCOUNT);
		}
	}

	@Test
	void validToMayEqualValidFromButNotPrecedeIt() {
		Discount discount = Discount.of(DiscountType.PERCENTAGE_DISCOUNT, BigDecimal.TEN, null);
		assertThat(new OfferDetails("REF", "Title", null, discount, null, TODAY, TODAY)).isNotNull();
		assertFails(() -> new OfferDetails("REF", "Title", null, discount, null, TODAY, TODAY.minusDays(1)),
				ErrorCode.INVALID_DATE_RANGE);
		assertFails(() -> new OfferDetails("bad ref!", "Title", null, discount, null, TODAY, TODAY), ErrorCode.INVALID_OFFER);
		assertFails(() -> new OfferDetails("REF", "Title", null, discount, 0, TODAY, TODAY), ErrorCode.INVALID_OFFER);
	}

}
