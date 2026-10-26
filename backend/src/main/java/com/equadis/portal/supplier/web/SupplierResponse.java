package com.equadis.portal.supplier.web;

public record SupplierResponse(Long id, String companyName, Contact contact, String countryCode) {

	public record Contact(String name, String email, String phone) {
	}

}
