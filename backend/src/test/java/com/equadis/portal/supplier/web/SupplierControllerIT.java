package com.equadis.portal.supplier.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.support.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

/** Full stack: HTTP → service → PostgreSQL (Testcontainers), checking status codes and error codes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class SupplierControllerIT {

	@Autowired
	MockMvc mvc;

	@Test
	void createReadUpdateDelete() throws Exception {
		String name = uniqueName("Maison Lumière");

		String created = mvc.perform(asAdmin(post("/api/suppliers")).content(body(name, "fr")))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/suppliers/")))
			.andExpect(jsonPath("$.companyName").value(name))
			.andExpect(jsonPath("$.countryCode").value("FR"))
			.andExpect(jsonPath("$.contact.email").value("claire@maison.fr"))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long id = ((Number) JsonPath.read(created, "$.id")).longValue();

		mvc.perform(asAdmin(get("/api/suppliers/{id}", id))).andExpect(status().isOk());

		mvc.perform(asAdmin(put("/api/suppliers/{id}", id)).content(body(name + " BE", "BE")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.companyName").value(name + " BE"))
			.andExpect(jsonPath("$.countryCode").value("BE"));

		mvc.perform(asAdmin(get("/api/suppliers")).param("q", name + " BE"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(id));

		mvc.perform(asAdmin(delete("/api/suppliers/{id}", id))).andExpect(status().isNoContent());

		mvc.perform(asAdmin(get("/api/suppliers/{id}", id)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("SUPPLIER_NOT_FOUND"));
	}

	@Test
	void companyNameIsUniqueIgnoringCase() throws Exception {
		String name = uniqueName("Nordic Pure");
		mvc.perform(asAdmin(post("/api/suppliers")).content(body(name, "SE"))).andExpect(status().isCreated());

		mvc.perform(asAdmin(post("/api/suppliers")).content(body("  " + name.toUpperCase() + " ", "SE")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SUPPLIER_NAME_TAKEN"));
	}

	@Test
	void renamingOntoAnotherSupplierIsRejectedButKeepingTheOwnNameIsNot() throws Exception {
		String first = uniqueName("Alpina Foods");
		String second = uniqueName("Bella Pasta");
		mvc.perform(asAdmin(post("/api/suppliers")).content(body(first, "CH"))).andExpect(status().isCreated());
		String created = mvc.perform(asAdmin(post("/api/suppliers")).content(body(second, "IT")))
			.andReturn()
			.getResponse()
			.getContentAsString();
		long secondId = ((Number) JsonPath.read(created, "$.id")).longValue();

		mvc.perform(asAdmin(put("/api/suppliers/{id}", secondId)).content(body(first.toLowerCase(), "IT")))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SUPPLIER_NAME_TAKEN"));
		mvc.perform(asAdmin(put("/api/suppliers/{id}", secondId)).content(body(second, "FR"))).andExpect(status().isOk());
	}

	@Test
	void onlyTheAdministratorCanWrite() throws Exception {
		mvc.perform(post("/api/suppliers").header(CurrentActor.HEADER, "SUPPLIER:1")
			.contentType(MediaType.APPLICATION_JSON)
			.content(body(uniqueName("Sneaky"), "FR")))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));
	}

	@Test
	void personaHeaderIsRequiredAndChecked() throws Exception {
		mvc.perform(get("/api/suppliers"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("ACTING_AS_REQUIRED"));
		mvc.perform(get("/api/suppliers").header(CurrentActor.HEADER, "BOSS"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_ACTING_AS"));
	}

	@Test
	void invalidRequestListsFieldErrors() throws Exception {
		String invalid = """
				{"companyName": "", "contact": {"name": "", "email": "nope", "phone": "abc"}, "countryCode": "FRA"}
				""";

		mvc.perform(asAdmin(post("/api/suppliers")).content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("companyName")))
			.andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("contact.email")))
			.andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("countryCode")));
	}

	@Test
	void unknownCountryAndSortAreRejected() throws Exception {
		mvc.perform(asAdmin(post("/api/suppliers")).content(body(uniqueName("Nowhere Ltd"), "XX")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_COUNTRY_CODE"));
		mvc.perform(asAdmin(get("/api/suppliers")).param("sort", "version"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SORT"));
	}

	@Test
	void personaTableHoldsTheThreePersonas() throws Exception {
		mvc.perform(get("/api/personas"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.length()").value(3))
			.andExpect(jsonPath("$[*].code").value(hasItem("ADMIN")))
			.andExpect(jsonPath("$[*].code").value(hasItem("SUPPLIER")))
			.andExpect(jsonPath("$[*].code").value(hasItem("RETAILER")));
	}

	private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return request.header(CurrentActor.HEADER, "ADMIN").contentType(MediaType.APPLICATION_JSON);
	}

	private static String body(String companyName, String countryCode) {
		return """
				{"companyName": "%s", "contact": {"name": "Claire Dubois", "email": "claire@maison.fr", "phone": "+33 1 42 68 53 00"}, "countryCode": "%s"}
				""".formatted(companyName, countryCode);
	}

	/** Tests share one database, so each one uses its own names. */
	private static String uniqueName(String base) {
		return base + " " + UUID.randomUUID().toString().substring(0, 8);
	}

}
