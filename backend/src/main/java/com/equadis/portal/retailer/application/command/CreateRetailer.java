package com.equadis.portal.retailer.application.command;

import com.equadis.portal.retailer.domain.Location;
import com.equadis.portal.retailer.domain.StoreType;
import com.equadis.portal.shared.domain.Email;

public record CreateRetailer(String storeName, StoreType storeType, Location location, Email contactEmail) {

}
