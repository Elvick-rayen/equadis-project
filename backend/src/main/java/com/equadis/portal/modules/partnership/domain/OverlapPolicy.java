package com.equadis.portal.modules.partnership.domain;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.DateRange;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Domain service for BR-04: the same supplier and retailer cannot have two partnerships with
 * overlapping periods (terminated ones excluded). A rule across several aggregates, so it
 * cannot live inside one Partnership. The database exclusion constraint is the last guard.
 */
public class OverlapPolicy {

	private final PartnershipRepository partnerships;

	public OverlapPolicy(PartnershipRepository partnerships) {
		this.partnerships = partnerships;
	}

	/** For a new partnership. */
	public void requireNoOverlap(long supplierId, long retailerId, DateRange period) {
		check(supplierId, retailerId, period, 0L);
	}

	/** For an existing partnership whose period changes (renewal). */
	public void requireNoOverlap(Partnership partnership, DateRange newPeriod) {
		check(partnership.supplierId(), partnership.retailerId(), newPeriod, partnership.id());
	}

	private void check(long supplierId, long retailerId, DateRange period, long excludedId) {
		if (partnerships.existsOverlapping(supplierId, retailerId, period.start(), period.end(), excludedId)) {
			throw new BusinessException(ErrorCode.PARTNERSHIP_OVERLAP,
					"This supplier and retailer already have a partnership overlapping " + period.start() + " – "
							+ period.end());
		}
	}

}
