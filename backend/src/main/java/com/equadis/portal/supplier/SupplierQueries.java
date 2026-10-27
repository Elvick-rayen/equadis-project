package com.equadis.portal.supplier;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Public API of the supplier module: what other modules may ask about suppliers. */
public interface SupplierQueries {

	boolean exists(long supplierId);

	Optional<SupplierSummary> findSummary(long supplierId);

	/** All suppliers, sorted by company name. */
	List<SupplierSummary> findAllSummaries();

	List<SupplierSummary> findSummaries(Collection<Long> supplierIds);

	/** Company names are unique ignoring case (BR-01), so there is at most one match. */
	Optional<SupplierSummary> findByCompanyName(String companyName);

}
