package com.equadis.portal.auth.application.command;

import com.equadis.portal.retailer.application.command.CreateRetailer;
import com.equadis.portal.supplier.application.command.CreateSupplier;

/** Sign up as a new supplier or retailer: exactly one of {@code supplier} and {@code retailer} is set. */
public record SignUp(String username, String password, CreateSupplier supplier, CreateRetailer retailer) {

	public static SignUp asSupplier(String username, String password, CreateSupplier supplier) {
		return new SignUp(username, password, supplier, null);
	}

	public static SignUp asRetailer(String username, String password, CreateRetailer retailer) {
		return new SignUp(username, password, null, retailer);
	}

}
