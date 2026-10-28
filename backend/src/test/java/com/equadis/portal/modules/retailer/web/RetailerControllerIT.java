package com.equadis.portal.modules.retailer.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.nullValue;
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
import com.equadis.portal.support.TestTokens;
import com.equadis.portal.support.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

/** Retailer CRUD end to end: HTTP → service → PostgreSQL (Testcontainers), with JWT personas. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfig.class, TestTokens.class })
class RetailerControllerIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	TestTokens tokens;

	@Test
	void createReadUpdateDelete() throws Exception {
		String name = unique("City Market");

		long id = createdId(mvc.perform(asAdmin(post("/api/retailers")).content(physical(name, "Lyon")))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", startsWith("/api/retailers/")))
			.andExpect(jsonPath("$.storeName").value(name))
			.andExpect(jsonPath("$.storeType").value("PHYSICAL"))
			.andExpect(jsonPath("$.location.city").value("Lyon"))
			.andExpect(jsonPath("$.location.website").value(nullValue()))
			.andReturn()
			.getResponse()
			.getContentAsString());

		mvc.perform(asAdmin(get("/api/retailers/{id}", id))).andExpect(status().isOk());

		// Becomes an online store: the website is required, the street address is dropped.
		mvc.perform(asAdmin(put("/api/retailers/{id}", id)).content(online(name + " Online", "https://shop.example.fr")))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.storeType").value("ONLINE"))
			.andExpect(jsonPath("$.location.website").value("https://shop.example.fr"))
			.andExpect(jsonPath("$.location.city").value(nullValue()));

		mvc.perform(asAdmin(get("/api/retailers")).param("q", name + " Online"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.content[0].id").value(id));

		mvc.perform(asAdmin(delete("/api/retailers/{id}", id))).andExpect(status().isNoContent());

		mvc.perform(asAdmin(get("/api/retailers/{id}", id)))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RETAILER_NOT_FOUND"));
	}

	@Test
	void storeTypeRulesAreEnforced() throws Exception {
		// BR-02: an online store needs a website, a physical store a city and an address.
		mvc.perform(asAdmin(post("/api/retailers")).content(online(unique("No Site"), "")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_LOCATION"));
		mvc.perform(asAdmin(post("/api/retailers")).content(physical(unique("No City"), "")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_LOCATION"));
	}

	@Test
	void invalidRequestListsFieldErrors() throws Exception {
		String invalid = """
				{"storeName": "", "storeType": null, "countryCode": "FRA", "contactEmail": "nope"}
				""";
		mvc.perform(asAdmin(post("/api/retailers")).content(invalid))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
			.andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("storeName")))
			.andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("storeType")))
			.andExpect(jsonPath("$.fieldErrors[*].field").value(hasItem("contactEmail")));
		mvc.perform(asAdmin(get("/api/retailers")).param("sort", "version"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SORT"));
	}

	@Test
	void aRetailerOnlySeesItsOwnDataAndCannotWrite() throws Exception {
		long ownId = createdId(mvc.perform(asAdmin(post("/api/retailers")).content(physical(unique("Own"), "Paris")))
			.andReturn()
			.getResponse()
			.getContentAsString());
		CurrentActor retailer = CurrentActor.retailer(ownId);

		mvc.perform(tokens.as(retailer, get("/api/retailers/{id}", ownId))).andExpect(status().isOk());
		mvc.perform(tokens.as(retailer, get("/api/retailers/{id}", ownId + 1000)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));
		mvc.perform(tokens.as(retailer, get("/api/retailers"))).andExpect(status().isForbidden());
		mvc.perform(tokens.as(retailer, put("/api/retailers/{id}", ownId)).contentType(MediaType.APPLICATION_JSON)
			.content(physical(unique("Renamed"), "Paris"))).andExpect(status().isForbidden());
		mvc.perform(tokens.as(retailer, delete("/api/retailers/{id}", ownId))).andExpect(status().isForbidden());
		mvc.perform(tokens.as(CurrentActor.supplier(1), get("/api/retailers/{id}", ownId)))
			.andExpect(status().isForbidden());
	}

	@Test
	void updatingOrDeletingAnUnknownRetailerIs404() throws Exception {
		mvc.perform(asAdmin(put("/api/retailers/{id}", 999999)).content(physical(unique("Ghost"), "Lyon")))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("RETAILER_NOT_FOUND"));
		mvc.perform(asAdmin(delete("/api/retailers/{id}", 999999))).andExpect(status().isNotFound());
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return tokens.as(CurrentActor.admin(), request).contentType(MediaType.APPLICATION_JSON);
	}

	private static String physical(String storeName, String city) {
		return """
				{"storeName": "%s", "storeType": "PHYSICAL", "countryCode": "FR", "city": "%s",
				 "address": "12 Rue de la République", "contactEmail": "contact@store.fr"}
				""".formatted(storeName, city);
	}

	private static String online(String storeName, String website) {
		return """
				{"storeName": "%s", "storeType": "ONLINE", "countryCode": "FR", "website": "%s",
				 "contactEmail": "contact@store.fr"}
				""".formatted(storeName, website);
	}

	private static long createdId(String json) {
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	/** Tests share one database, so each one uses its own names. */
	private static String unique(String base) {
		return base + " " + UUID.randomUUID().toString().substring(0, 8);
	}

}
