package com.equadis.portal.modules.auth.application.command;

import com.equadis.portal.modules.retailer.NewRetailer;
import com.equadis.portal.modules.supplier.NewSupplier;

/** Sign up as a new supplier or retailer: exactly one of {@code supplier} and {@code retailer} is set. */
public record SignUp(String username, String password, NewSupplier supplier, NewRetailer retailer) {

	public static SignUp asSupplier(String username, String password, NewSupplier supplier) {
		return new SignUp(username, password, supplier, null);
	}

	public static SignUp asRetailer(String username, String password, NewRetailer retailer) {
		return new SignUp(username, password, null, retailer);
	}

}
