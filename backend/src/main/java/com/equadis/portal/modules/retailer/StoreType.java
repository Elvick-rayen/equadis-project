package com.equadis.portal.modules.retailer;

public enum StoreType {

	ONLINE, PHYSICAL, HYBRID;

	public boolean needsWebsite() {
		return this != PHYSICAL;
	}

	public boolean needsStreetAddress() {
		return this != ONLINE;
	}

}
