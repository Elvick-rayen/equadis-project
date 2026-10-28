package com.equadis.portal.modules.partnership.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.support.TestTokens;
import com.equadis.portal.support.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

/** Partnership API end to end (PostgreSQL via Testcontainers, so the overlap exclusion constraint is real). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfig.class, TestTokens.class })
class PartnershipControllerIT {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Paris"));

	@Autowired
	MockMvc mvc;

	@Autowired
	TestTokens tokens;

	private long supplierId;

	private long retailerId;

	@BeforeEach
	void createCompanies() throws Exception {
		supplierId = idOf(mvc.perform(asAdmin(post("/api/suppliers")).content("""
				{"companyName": "%s", "contact": {"name": "Claire", "email": "claire@maison.fr", "phone": "+33 1 42 68 53 00"},
				 "countryCode": "FR"}
				""".formatted(unique("Supplier")))).andExpect(status().isCreated()));
		retailerId = idOf(mvc.perform(asAdmin(post("/api/retailers")).content("""
				{"storeName": "%s", "storeType": "PHYSICAL", "countryCode": "FR", "city": "Lyon",
				 "address": "12 Rue de la République", "contactEmail": "contact@store.fr"}
				""".formatted(unique("Retailer")))).andExpect(status().isCreated()));
	}

	@Test
	void createRenewAndTerminate() throws Exception {
		long id = idOf(create(TODAY, TODAY.plusMonths(6)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("ACTIVE"))
			.andExpect(jsonPath("$.allowedActions", containsInAnyOrder("RENEW", "TERMINATE")))
			.andExpect(jsonPath("$.renewals", empty())));

		mvc.perform(asAdmin(post("/api/partnerships/{id}/renewals", id))
			.content("{\"newEndDate\": \"%s\"}".formatted(TODAY.plusMonths(12))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.endDate").value(TODAY.plusMonths(12).toString()))
			.andExpect(jsonPath("$.renewals[0].previousEndDate").value(TODAY.plusMonths(6).toString()));

		mvc.perform(asAdmin(post("/api/partnerships/{id}/termination", id)).content("{\"reason\": \"\"}"))
			.andExpect(status().isBadRequest());
		mvc.perform(asAdmin(post("/api/partnerships/{id}/termination", id)).content("{\"reason\": \"Contract ended\"}"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("TERMINATED"))
			.andExpect(jsonPath("$.terminationReason").value("Contract ended"))
			.andExpect(jsonPath("$.allowedActions", empty()));

		mvc.perform(asAdmin(post("/api/partnerships/{id}/renewals", id))
			.content("{\"newEndDate\": \"%s\"}".formatted(TODAY.plusMonths(18))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RENEWAL_NOT_ALLOWED"));
	}

	@Test
	void periodRulesAndOverlap() throws Exception {
		create(TODAY.minusDays(1), TODAY.plusMonths(1)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("START_DATE_IN_PAST"));
		create(TODAY, TODAY.plusMonths(37)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("PARTNERSHIP_TOO_LONG"));
		create(TODAY.plusDays(5), TODAY.plusDays(5)).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DATE_RANGE"));

		long first = idOf(create(TODAY, TODAY.plusMonths(3)).andExpect(status().isCreated()));
		// BR-04: sharing even one day with a non-terminated partnership is refused...
		create(TODAY.plusMonths(3), TODAY.plusMonths(6)).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PARTNERSHIP_OVERLAP"));
		// ...the next day is fine, and a renewal into it is refused.
		create(TODAY.plusMonths(3).plusDays(1), TODAY.plusMonths(6)).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("SCHEDULED"));
		mvc.perform(asAdmin(post("/api/partnerships/{id}/renewals", first))
			.content("{\"newEndDate\": \"%s\"}".formatted(TODAY.plusMonths(4))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("PARTNERSHIP_OVERLAP"));

		// Terminated partnerships no longer block the period.
		mvc.perform(asAdmin(post("/api/partnerships/{id}/termination", first)).content("{\"reason\": \"Replaced\"}"))
			.andExpect(status().isOk());
		create(TODAY, TODAY.plusMonths(2)).andExpect(status().isCreated());
	}

	@Test
	void unknownCompaniesAreRefused() throws Exception {
		mvc.perform(asAdmin(post("/api/partnerships")).content(createBody(999999, retailerId, TODAY, TODAY.plusMonths(1))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("SUPPLIER_NOT_FOUND"));
	}

	@Test
	void listsFilterByComputedStatusAndExpiry() throws Exception {
		long active = idOf(create(TODAY, TODAY.plusDays(10)).andExpect(status().isCreated()));
		long scheduled = idOf(create(TODAY.plusMonths(1), TODAY.plusMonths(2)).andExpect(status().isCreated()));

		mvc.perform(asAdmin(get("/api/partnerships")).param("status", "SCHEDULED").param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItem((int) scheduled)))
			.andExpect(jsonPath("$.content[*].id", not(hasItem((int) active))));
		mvc.perform(asAdmin(get("/api/partnerships")).param("expiringWithinDays", "30").param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[*].id", hasItem((int) active)))
			.andExpect(jsonPath("$.content[*].id", not(hasItem((int) scheduled))));
		mvc.perform(asAdmin(get("/api/partnerships")).param("sort", "supplierId"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SORT"));
	}

	@Test
	void linkedCompaniesAndAccessRules() throws Exception {
		long id = idOf(create(TODAY, TODAY.plusMonths(6)).andExpect(status().isCreated()));

		mvc.perform(tokens.as(CurrentActor.supplier(supplierId), get("/api/suppliers/{id}/retailers", supplierId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) retailerId)));
		mvc.perform(tokens.as(CurrentActor.retailer(retailerId), get("/api/retailers/{id}/suppliers", retailerId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) supplierId)));

		// Either company may read the partnership; nobody else, and only the administrator writes.
		mvc.perform(tokens.as(CurrentActor.supplier(supplierId), get("/api/partnerships/{id}", id))).andExpect(status().isOk());
		mvc.perform(tokens.as(CurrentActor.supplier(supplierId + 1000), get("/api/partnerships/{id}", id)))
			.andExpect(status().isForbidden());
		mvc.perform(tokens.as(CurrentActor.retailer(retailerId), get("/api/suppliers/{id}/retailers", supplierId)))
			.andExpect(status().isForbidden());
		mvc.perform(tokens.as(CurrentActor.supplier(supplierId), post("/api/partnerships/{id}/termination", id))
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"reason\": \"Mine\"}")).andExpect(status().isForbidden());
	}

	@Test
	void companiesWithPartnershipsCannotBeDeleted() throws Exception {
		create(TODAY, TODAY.plusMonths(6)).andExpect(status().isCreated());

		mvc.perform(asAdmin(delete("/api/suppliers/{id}", supplierId)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SUPPLIER_IN_USE"));
		mvc.perform(asAdmin(delete("/api/retailers/{id}", retailerId)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("RETAILER_IN_USE"));
	}

	private ResultActions create(LocalDate start, LocalDate end) throws Exception {
		return mvc.perform(asAdmin(post("/api/partnerships")).content(createBody(supplierId, retailerId, start, end)));
	}

	private static String createBody(long supplierId, long retailerId, LocalDate start, LocalDate end) {
		return """
				{"supplierId": %d, "retailerId": %d, "startDate": "%s", "endDate": "%s"}
				""".formatted(supplierId, retailerId, start, end);
	}

	private MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder request) {
		return tokens.as(CurrentActor.admin(), request).contentType(MediaType.APPLICATION_JSON);
	}

	private static long idOf(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

	private static String unique(String base) {
		return base + " " + UUID.randomUUID().toString().substring(0, 8);
	}

}
