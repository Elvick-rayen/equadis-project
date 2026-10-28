package com.equadis.portal.modules.subscription.web;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.support.TestTokens;
import com.equadis.portal.support.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

/**
 * Offers and subscriptions end to end, across three modules: BR-08 to BR-12, and the
 * cancellations triggered by closing an offer (BR-08) or terminating a partnership (BR-07).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({ TestcontainersConfig.class, TestTokens.class })
class OfferSubscriptionFlowIT {

	private static final LocalDate TODAY = LocalDate.now(ZoneId.of("Europe/Paris"));

	@Autowired
	MockMvc mvc;

	@Autowired
	TestTokens tokens;

	@Autowired
	JdbcTemplate jdbc;

	private long supplierId;

	private long retailerId;

	private long otherRetailerId;

	private long partnershipId;

	@BeforeEach
	void setUp() throws Exception {
		supplierId = idOf(mvc.perform(as(CurrentActor.admin(), post("/api/suppliers")).content("""
				{"companyName": "%s", "contact": {"name": "Lukas", "email": "lukas@alpina.ch", "phone": "+41 44 210 33 10"},
				 "countryCode": "CH"}
				""".formatted(unique("Alpina")))));
		retailerId = createRetailer();
		otherRetailerId = createRetailer();
		partnershipId = idOf(mvc.perform(as(CurrentActor.admin(), post("/api/partnerships")).content("""
				{"supplierId": %d, "retailerId": %d, "startDate": "%s", "endDate": "%s"}
				""".formatted(supplierId, retailerId, TODAY, TODAY.plusMonths(6)))));
	}

	@Test
	void supplierLifecycleAndDiscountRules() throws Exception {
		long offerId = idOf(createOffer("ALP-01", percentage("12")).andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("DRAFT"))
			.andExpect(jsonPath("$.allowedActions", containsInAnyOrder("EDIT", "PUBLISH"))));

		// BR-09
		createOffer("ALP-02", percentage("0")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DISCOUNT"));
		createOffer("ALP-03", percentage("101")).andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DISCOUNT"));
		createOffer("ALP-04", "\"discountType\": \"FIXED_AMOUNT_DISCOUNT\", \"discountValue\": 250")
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_DISCOUNT"));
		createOffer("ALP-05", "\"discountType\": \"FIXED_AMOUNT_DISCOUNT\", \"discountValue\": 250, \"currency\": \"EUR\"")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.currency").value("EUR"));
		createOffer("alp-01", percentage("5")).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("OFFER_REFERENCE_TAKEN"));

		// BR-08: edit while draft, then publish, then no more edits.
		mvc.perform(asSupplier(put("/api/suppliers/{s}/offers/{o}", supplierId, offerId)).content(offerBody("ALP-01", percentage("15"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.discountValue").value(15.0));
		mvc.perform(asSupplier(post("/api/suppliers/{s}/offers/{o}/publish", supplierId, offerId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("PUBLISHED"))
			.andExpect(jsonPath("$.allowedActions", contains("CLOSE")));
		mvc.perform(asSupplier(put("/api/suppliers/{s}/offers/{o}", supplierId, offerId)).content(offerBody("ALP-01", percentage("20"))))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("OFFER_NOT_EDITABLE"));

		// Another supplier cannot see or touch these offers.
		mvc.perform(as(CurrentActor.supplier(supplierId + 1000), get("/api/suppliers/{s}/offers", supplierId)))
			.andExpect(status().isForbidden());
	}

	@Test
	void retailersSeeAndSubscribeOnlyToAvailableOffers() throws Exception {
		long published = publishedOffer("ALP-10");
		idOf(createOffer("ALP-11", percentage("5")));

		// BR-10: only the published offer, and only for a partner retailer.
		mvc.perform(as(CurrentActor.retailer(retailerId), get("/api/retailers/{r}/available-offers", retailerId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[*].id", contains((int) published)));
		mvc.perform(as(CurrentActor.retailer(otherRetailerId), get("/api/retailers/{r}/available-offers", otherRetailerId)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", empty()));
		subscribe(otherRetailerId, published).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("OFFER_NOT_AVAILABLE"));

		// BR-11: one active subscription per offer.
		long subscription = idOf(subscribe(retailerId, published).andExpect(status().isCreated())
			.andExpect(jsonPath("$.state").value("ACTIVE")));
		subscribe(retailerId, published).andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("ALREADY_SUBSCRIBED"));

		// Unsubscribe, then subscribing again is allowed.
		mvc.perform(as(CurrentActor.retailer(retailerId), delete("/api/retailers/{r}/subscriptions/{s}", retailerId, subscription)))
			.andExpect(status().isNoContent());
		subscribe(retailerId, published).andExpect(status().isCreated());
		mySubscriptions().andExpect(jsonPath("$[*].state", containsInAnyOrder("ACTIVE", "CANCELLED")))
			.andExpect(jsonPath("$[*].cancellationCause", containsInAnyOrder(null, "RETAILER_REQUEST")));

		// A retailer cannot act for another one.
		mvc.perform(as(CurrentActor.retailer(otherRetailerId), get("/api/retailers/{r}/subscriptions", retailerId)))
			.andExpect(status().isForbidden());
	}

	@Test
	void closingAnOfferCancelsItsSubscriptions() throws Exception {
		long offer = publishedOffer("ALP-20");
		subscribe(retailerId, offer).andExpect(status().isCreated());

		mvc.perform(asSupplier(post("/api/suppliers/{s}/offers/{o}/close", supplierId, offer))).andExpect(status().isOk());

		mySubscriptions().andExpect(jsonPath("$[0].state").value("CANCELLED"))
			.andExpect(jsonPath("$[0].cancellationCause").value("OFFER_CLOSED"));
	}

	@Test
	void terminatingThePartnershipCancelsTheRetailersSubscriptions() throws Exception {
		subscribe(retailerId, publishedOffer("ALP-30")).andExpect(status().isCreated());
		subscribe(retailerId, publishedOffer("ALP-31")).andExpect(status().isCreated());

		mvc.perform(as(CurrentActor.admin(), post("/api/partnerships/{id}/termination", partnershipId))
			.content("{\"reason\": \"Contract ended\"}")).andExpect(status().isOk());

		mySubscriptions().andExpect(jsonPath("$[*].cancellationCause", contains("PARTNERSHIP_TERMINATED", "PARTNERSHIP_TERMINATED")));
		mvc.perform(as(CurrentActor.retailer(retailerId), get("/api/retailers/{r}/available-offers", retailerId)))
			.andExpect(jsonPath("$", empty()));
	}

	@Test
	void subscriptionsAreSuspendedWhileThePartnershipIsExpiredAndResumeOnRenewal() throws Exception {
		subscribe(retailerId, publishedOffer("ALP-40")).andExpect(status().isCreated());

		// Move the partnership into the past: it is now EXPIRED (BR-05), ended yesterday.
		jdbc.update("update partnership set start_date = ?, end_date = ? where id = ?", TODAY.minusMonths(6),
				TODAY.minusDays(1), partnershipId);
		mySubscriptions().andExpect(jsonPath("$[0].state").value("SUSPENDED"));

		// BR-06: an expired partnership can be renewed within 30 days, and BR-12: subscriptions resume.
		mvc.perform(as(CurrentActor.admin(), post("/api/partnerships/{id}/renewals", partnershipId))
			.content("{\"newEndDate\": \"%s\"}".formatted(TODAY.plusMonths(3)))).andExpect(status().isOk());
		mySubscriptions().andExpect(jsonPath("$[0].state").value("ACTIVE"));
	}

	@Test
	void companiesWithOffersOrSubscriptionsCannotBeDeleted() throws Exception {
		mvc.perform(as(CurrentActor.admin(), delete("/api/retailers/{id}", otherRetailerId))).andExpect(status().isNoContent());
		publishedOffer("ALP-50");
		mvc.perform(as(CurrentActor.admin(), delete("/api/suppliers/{id}", supplierId)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("SUPPLIER_IN_USE"));
	}

	// --- helpers ---

	private long createRetailer() throws Exception {
		return idOf(mvc.perform(as(CurrentActor.admin(), post("/api/retailers")).content("""
				{"storeName": "%s", "storeType": "ONLINE", "countryCode": "FR", "website": "https://shop.example.fr",
				 "contactEmail": "contact@store.fr"}
				""".formatted(unique("Retailer")))));
	}

	private long publishedOffer(String reference) throws Exception {
		long id = idOf(createOffer(reference, percentage("10")).andExpect(status().isCreated()));
		mvc.perform(asSupplier(post("/api/suppliers/{s}/offers/{o}/publish", supplierId, id))).andExpect(status().isOk());
		return id;
	}

	private ResultActions createOffer(String reference, String discount) throws Exception {
		return mvc.perform(asSupplier(post("/api/suppliers/{s}/offers", supplierId)).content(offerBody(reference, discount)));
	}

	private ResultActions subscribe(long retailer, long offerId) throws Exception {
		return mvc.perform(as(CurrentActor.retailer(retailer), post("/api/retailers/{r}/subscriptions", retailer))
			.content("{\"offerId\": %d}".formatted(offerId)));
	}

	private ResultActions mySubscriptions() throws Exception {
		return mvc.perform(as(CurrentActor.retailer(retailerId), get("/api/retailers/{r}/subscriptions", retailerId)))
			.andExpect(status().isOk());
	}

	private static String percentage(String value) {
		return "\"discountType\": \"PERCENTAGE_DISCOUNT\", \"discountValue\": " + value;
	}

	private static String offerBody(String reference, String discount) {
		return """
				{"reference": "%s", "title": "Winter cereal promotion", %s,
				 "validFrom": "%s", "validTo": "%s"}
				""".formatted(reference, discount, TODAY, TODAY.plusDays(30));
	}

	private MockHttpServletRequestBuilder asSupplier(MockHttpServletRequestBuilder request) {
		return as(CurrentActor.supplier(supplierId), request);
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
