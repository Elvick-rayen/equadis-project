package com.equadis.portal.modules.supplier;

/**
 * Public API of the supplier module for other modules that create suppliers (e.g. sign-up).
 * Enforces the same rules as the supplier screens, including BR-01.
 */
public interface SupplierRegistration {

	SupplierSummary register(NewSupplier supplier);

}
