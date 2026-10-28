package com.equadis.portal.modules.supplier.application.command;

import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.modules.supplier.domain.ContactInfo;

public record CreateSupplier(String companyName, ContactInfo contact, CountryCode countryCode) {

}
