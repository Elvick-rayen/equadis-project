package com.equadis.portal.modules.supplier.application.command;

import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.modules.supplier.domain.ContactInfo;

public record UpdateSupplier(long supplierId, String companyName, ContactInfo contact, CountryCode countryCode) {

}
