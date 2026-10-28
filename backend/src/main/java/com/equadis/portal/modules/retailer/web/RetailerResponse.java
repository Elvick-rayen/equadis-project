package com.equadis.portal.modules.retailer.web;

import com.equadis.portal.modules.retailer.StoreType;

/** A retailer as returned by /api/retailers. Location fields a store type does not use are null. */
public record RetailerResponse(Long id, String storeName, StoreType storeType, LocationResponse location,
		String contactEmail) {

	public record LocationResponse(String countryCode, String city, String address, String website) {
	}

}
