package com.equadis.portal.modules.supplier.web;

import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.modules.supplier.application.command.CreateSupplier;
import com.equadis.portal.modules.supplier.application.command.UpdateSupplier;
import com.equadis.portal.modules.supplier.domain.ContactInfo;
import com.equadis.portal.modules.supplier.domain.Supplier;

/** Maps between HTTP DTOs and the domain. */
final class SupplierWebMapper {

	private SupplierWebMapper() {
	}

	static CreateSupplier toCreate(SupplierRequest request) {
		return new CreateSupplier(request.companyName(), toContact(request.contact()), CountryCode.of(request.countryCode()));
	}

	static UpdateSupplier toUpdate(long supplierId, SupplierRequest request) {
		return new UpdateSupplier(supplierId, request.companyName(), toContact(request.contact()),
				CountryCode.of(request.countryCode()));
	}

	static SupplierResponse toResponse(Supplier supplier) {
		ContactInfo contact = supplier.contact();
		return new SupplierResponse(supplier.id(), supplier.companyName(),
				new SupplierResponse.Contact(contact.name(), contact.email(), contact.phone()),
				supplier.countryCode().value());
	}

	private static ContactInfo toContact(SupplierRequest.Contact contact) {
		return new ContactInfo(contact.name(), contact.email(), contact.phone());
	}

}
