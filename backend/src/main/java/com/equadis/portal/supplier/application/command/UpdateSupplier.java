package com.equadis.portal.supplier.application.command;

import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.supplier.domain.ContactInfo;

public record UpdateSupplier(long supplierId, String companyName, ContactInfo contact, CountryCode countryCode) {

}
