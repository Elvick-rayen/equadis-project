package com.equadis.portal.modules.retailer;

/**
 * Details of a retailer to register. Which location fields are required depends on the
 * store type (BR-02); the retailer module checks it.
 */
public record NewRetailer(String storeName, StoreType storeType, String countryCode, String city, String address,
		String website, String contactEmail) {

}
