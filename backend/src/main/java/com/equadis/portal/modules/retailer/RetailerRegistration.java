package com.equadis.portal.modules.retailer;

/** Public API of the retailer module for other modules that create retailers (e.g. sign-up). */
public interface RetailerRegistration {

	RetailerSummary register(NewRetailer retailer);

}
