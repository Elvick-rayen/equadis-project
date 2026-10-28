package com.equadis.portal.modules.supplier.domain;

import java.util.Optional;

/** Persistence port for the Supplier aggregate. Implemented by Spring Data in infrastructure. */
public interface SupplierRepository {

	/** Saves and flushes, so database constraints are checked immediately. */
	Supplier saveAndFlush(Supplier supplier);

	Optional<Supplier> findById(Long id);

	void delete(Supplier supplier);

	boolean existsByCompanyNameIgnoreCase(String companyName);

	boolean existsByCompanyNameIgnoreCaseAndIdNot(String companyName, Long id);

}
