package com.equadis.portal.supplier;

import java.util.Collection;
import java.util.List;

/** Public API of the supplier module: what other modules may ask about suppliers. */
public interface SupplierQueries {

	boolean exists(long supplierId);

	/** All suppliers, sorted by company name. */
	List<SupplierSummary> findAllSummaries();

	List<SupplierSummary> findSummaries(Collection<Long> supplierIds);

}
