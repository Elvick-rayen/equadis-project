package com.equadis.portal.supplier.application.command;

import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.supplier.domain.ContactInfo;

public record CreateSupplier(String companyName, ContactInfo contact, CountryCode countryCode) {

}
