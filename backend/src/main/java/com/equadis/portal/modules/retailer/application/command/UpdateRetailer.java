package com.equadis.portal.modules.retailer.application.command;

import com.equadis.portal.modules.retailer.StoreType;
import com.equadis.portal.modules.retailer.domain.Location;
import com.equadis.portal.shared.domain.Email;

public record UpdateRetailer(long retailerId, String storeName, StoreType storeType,
                             Location location, Email contactEmail) { }