package com.equadis.portal.modules.retailer.web;

import com.equadis.portal.modules.retailer.application.command.CreateRetailer;
import com.equadis.portal.modules.retailer.application.command.UpdateRetailer;
import com.equadis.portal.modules.retailer.domain.Location;
import com.equadis.portal.modules.retailer.domain.Retailer;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.Email;

/** Maps between HTTP DTOs and the domain. Building the Location here is where BR-02 gets checked. */
final class RetailerWebMapper {

	private RetailerWebMapper() {
	}

	static CreateRetailer toCreate(RetailerRequest request) {
		return new CreateRetailer(request.storeName(), request.storeType(), toLocation(request),
				Email.of(request.contactEmail()));
	}

	static UpdateRetailer toUpdate(long retailerId, RetailerRequest request) {
		return new UpdateRetailer(retailerId, request.storeName(), request.storeType(), toLocation(request),
				Email.of(request.contactEmail()));
	}

	static RetailerResponse toResponse(Retailer retailer) {
		Location location = retailer.location();
		return new RetailerResponse(retailer.id(), retailer.storeName(), retailer.storeType(),
				new RetailerResponse.LocationResponse(location.countryCode(), location.city(), location.address(),
						location.website()),
				retailer.contactEmail().value());
	}

	private static Location toLocation(RetailerRequest request) {
		return Location.forStoreType(request.storeType(), CountryCode.of(request.countryCode()), request.city(),
				request.address(), request.website());
	}

}
