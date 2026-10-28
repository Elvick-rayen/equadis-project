package com.equadis.portal.shared.domain;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A period of calendar days, both ends included. The end must be after the start.
 * All dates are calendar dates in the Europe/Paris zone (see ClockConfig).
 */
public record DateRange(LocalDate start, LocalDate end) {

	public DateRange {
		if (start == null || end == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Start and end dates are required");
		}
		if (!end.isAfter(start)) {
			throw new BusinessException(ErrorCode.INVALID_DATE_RANGE, "End date must be after start date");
		}
	}

	public static DateRange of(LocalDate start, LocalDate end) {
		return new DateRange(start, end);
	}

	public boolean contains(LocalDate day) {
		Objects.requireNonNull(day, "day");
		return !day.isBefore(start) && !day.isAfter(end);
	}

	public boolean overlaps(DateRange other) {
		return !start.isAfter(other.end) && !other.start.isAfter(end);
	}

}
