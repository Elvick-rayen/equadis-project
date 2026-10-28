package com.equadis.portal.modules.audit.web;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

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

/** Committed partnership and offer actions appear in the audit log; failed ones do not. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfig.class, TestTokens.class })
class AuditLogIT {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Paris"));

	@Autowired
	MockMvc mvc;

	@Autowired
	TestTokens tokens;

	@Test
	void recordsCommittedActionsWithTheirActor() throws Exception {
		long supplierId = idOf(mvc.perform(as(CurrentActor.admin(), post("/api/suppliers")).content("""
				{"companyName": "%s", "contact": {"name": "Lukas", "email": "l@alpina.ch", "phone": "+41 44 210 33 10"},
				 "countryCode": "CH"}
				""".formatted(unique("Audit Supplier")))));
		long retailerId = idOf(mvc.perform(as(CurrentActor.admin(), post("/api/retailers")).content("""
				{"storeName": "%s", "storeType": "ONLINE", "countryCode": "FR", "website": "https://a.example.fr",
				 "contactEmail": "a@store.fr"}
				""".formatted(unique("Audit Retailer")))));
		long partnershipId = idOf(mvc.perform(as(CurrentActor.admin(), post("/api/partnerships")).content("""
				{"supplierId": %d, "retailerId": %d, "startDate": "%s", "endDate": "%s"}
				""".formatted(supplierId, retailerId, TODAY, TODAY.plusMonths(6)))));
		mvc.perform(as(CurrentActor.admin(), post("/api/partnerships/{id}/termination", partnershipId))
			.content("{\"reason\": \"Audit check\"}")).andExpect(status().isOk());
		// A refused action (already terminated) must not be logged.
		mvc.perform(as(CurrentActor.admin(), post("/api/partnerships/{id}/termination", partnershipId))
			.content("{\"reason\": \"Twice\"}")).andExpect(status().isConflict());

		long offerId = idOf(mvc.perform(as(CurrentActor.supplier(supplierId), post("/api/suppliers/{s}/offers", supplierId))
			.content("""
					{"reference": "AUD-1", "title": "Audited offer", "discountType": "PERCENTAGE_DISCOUNT",
					 "discountValue": 10, "validFrom": "%s", "validTo": "%s"}
					""".formatted(TODAY, TODAY.plusDays(10)))));

		mvc.perform(as(CurrentActor.admin(), get("/api/audit-log")).param("subjectType", "PARTNERSHIP").param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[?(@.subjectId == %d)].action".formatted(partnershipId), hasItem("PARTNERSHIP_CREATED")))
			.andExpect(jsonPath("$.content[?(@.subjectId == %d)].action".formatted(partnershipId), hasItem("PARTNERSHIP_TERMINATED")))
			.andExpect(jsonPath("$.content[?(@.subjectId == %d && @.action == 'PARTNERSHIP_TERMINATED')].details"
				.formatted(partnershipId), hasItem("Reason: Audit check")))
			.andExpect(jsonPath("$.content[?(@.subjectId == %d)].action".formatted(partnershipId), hasSize(2)))
			.andExpect(jsonPath("$.content[?(@.subjectId == %d)].actor".formatted(partnershipId), hasItem("test-admin")));
		mvc.perform(as(CurrentActor.admin(), get("/api/audit-log")).param("subjectType", "OFFER").param("size", "100"))
			.andExpect(jsonPath("$.content[?(@.subjectId == %d)].action".formatted(offerId), hasItem("OFFER_CREATED")))
			.andExpect(jsonPath("$.content[?(@.subjectId == %d)].actor".formatted(offerId), hasItem("test-supplier")));

		mvc.perform(as(CurrentActor.supplier(supplierId), get("/api/audit-log"))).andExpect(status().isForbidden());
	}

	private MockHttpServletRequestBuilder as(CurrentActor actor, MockHttpServletRequestBuilder request) {
		return tokens.as(actor, request).contentType(MediaType.APPLICATION_JSON);
	}

	private static long idOf(ResultActions result) throws Exception {
		return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id")).longValue();
	}

	private static String unique(String base) {
		return base + " " + UUID.randomUUID().toString().substring(0, 8);
	}

}
