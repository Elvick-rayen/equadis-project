package com.equadis.portal.supplier.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.ErrorCode;

class SupplierTest {

	private static final ContactInfo CONTACT = new ContactInfo("Claire Dubois", "claire@maison.fr", "+33 1 42 68 53 00");

	@Test
	void registerTrimsTheCompanyName() {
		Supplier supplier = Supplier.register("  Maison Lumière  ", CONTACT, CountryCode.of("FR"));

		assertThat(supplier.companyName()).isEqualTo("Maison Lumière");
		assertThat(supplier.contact()).isEqualTo(CONTACT);
		assertThat(supplier.countryCode()).isEqualTo(CountryCode.of("FR"));
	}

	@Test
	void rejectsBlankCompanyName() {
		assertThatThrownBy(() -> Supplier.register("   ", CONTACT, CountryCode.of("FR")))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_SUPPLIER));
	}

	@Test
	void rejectsTooLongCompanyName() {
		String name = "x".repeat(Supplier.MAX_COMPANY_NAME_LENGTH + 1);

		assertThatThrownBy(() -> Supplier.register(name, CONTACT, CountryCode.of("FR")))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_SUPPLIER));
	}

	@Test
	void updateReplacesAllDetails() {
		Supplier supplier = Supplier.register("Maison Lumière", CONTACT, CountryCode.of("FR"));
		ContactInfo newContact = new ContactInfo("Marc Girard", "marc@maison.be", "+32 2 123 45 67");

		supplier.update("Maison Lumière Belgique", newContact, CountryCode.of("BE"));

		assertThat(supplier.companyName()).isEqualTo("Maison Lumière Belgique");
		assertThat(supplier.contact()).isEqualTo(newContact);
		assertThat(supplier.countryCode().value()).isEqualTo("BE");
	}

	@Test
	void contactInfoIsTrimmedAndValidated() {
		ContactInfo contact = new ContactInfo(" Claire ", " claire@maison.fr ", " +33 1 42 68 53 00 ");
		assertThat(contact).isEqualTo(new ContactInfo("Claire", "claire@maison.fr", "+33 1 42 68 53 00"));

		assertInvalidContact(() -> new ContactInfo("", "claire@maison.fr", "+33 1 42 68 53 00"));
		assertInvalidContact(() -> new ContactInfo("Claire", "not-an-email", "+33 1 42 68 53 00"));
		assertInvalidContact(() -> new ContactInfo("Claire", "claire@maison.fr", "call me"));
	}

	@Test
	void countryCodeIsNormalizedAndMustBeIso() {
		assertThat(CountryCode.of(" fr ").value()).isEqualTo("FR");

		assertThatThrownBy(() -> CountryCode.of("XX"))
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_COUNTRY_CODE));
		assertThatThrownBy(() -> CountryCode.of(null)).isInstanceOf(BusinessException.class);
	}

	private static void assertInvalidContact(Runnable creation) {
		assertThatThrownBy(creation::run)
			.isInstanceOfSatisfying(BusinessException.class, ex -> assertThat(ex.code()).isEqualTo(ErrorCode.INVALID_CONTACT));
	}

}
