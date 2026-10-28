package com.equadis.portal.modules.offer.domain;

import java.math.BigDecimal;
import java.util.Currency;

import com.equadis.portal.modules.offer.DiscountType;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * An offer's discount: a percentage, or a fixed amount in a currency (BR-09). A sealed type, so
 * code handling a discount must handle both kinds, and each kind validates itself.
 */
public sealed interface Discount permits Discount.Percentage, Discount.FixedAmount {

	DiscountType type();

	BigDecimal value();

	/** Null for a percentage. */
	Currency currency();

	static Discount of(DiscountType type, BigDecimal value, String currencyCode) {
		if (type == null) {
			throw invalid("A discount type is required");
		}
		return switch (type) {
			case PERCENTAGE_DISCOUNT -> {
				if (currencyCode != null && !currencyCode.isBlank()) {
					throw invalid("A percentage discount has no currency");
				}
				yield new Percentage(value);
			}
			case FIXED_AMOUNT_DISCOUNT -> new FixedAmount(value, currency(currencyCode));
		};
	}

	/** BR-09: more than 0 and at most 100. */
	record Percentage(BigDecimal value) implements Discount {

		private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

		public Percentage {
			if (value == null || value.signum() <= 0 || value.compareTo(HUNDRED) > 0) {
				throw invalid("A percentage must be more than 0 and at most 100");
			}
			value = scale(value);
		}

		@Override
		public DiscountType type() {
			return DiscountType.PERCENTAGE_DISCOUNT;
		}

		@Override
		public Currency currency() {
			return null;
		}

	}

	/** BR-09: a positive amount, with a currency. */
	record FixedAmount(BigDecimal value, Currency currency) implements Discount {

		public FixedAmount {
			if (value == null || value.signum() <= 0) {
				throw invalid("A fixed amount must be positive");
			}
			if (currency == null) {
				throw invalid("A fixed amount needs a currency");
			}
			value = scale(value);
		}

		@Override
		public DiscountType type() {
			return DiscountType.FIXED_AMOUNT_DISCOUNT;
		}

	}

	private static Currency currency(String code) {
		if (code == null || code.isBlank()) {
			throw invalid("A fixed amount needs a currency");
		}
		try {
			return Currency.getInstance(code.trim().toUpperCase(java.util.Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw invalid("Unknown ISO 4217 currency: '" + code + "'");
		}
	}

	private static BigDecimal scale(BigDecimal value) {
		if (value.scale() > 2) {
			throw invalid("At most 2 decimals");
		}
		return value.setScale(2);
	}

	private static BusinessException invalid(String message) {
		return new BusinessException(ErrorCode.INVALID_DISCOUNT, message);
	}

}
