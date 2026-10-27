package com.equadis.portal.auth.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.equadis.portal.auth.domain.UserAccount;
import com.equadis.portal.auth.domain.UserAccountRepository;
import com.equadis.portal.support.TestcontainersConfig;
import com.jayway.jsonpath.JsonPath;

/** Sign up, sign in, tokens and the administrator-only persona switch, end to end. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
class AuthControllerIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserAccountRepository users;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void supplierSignUpCreatesTheCompanyAndSignsIn() throws Exception {
		String username = unique("alpina");
		String company = unique("Alpina Foods");

		String session = signUpSupplier(username, "12345678", company)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.token").isNotEmpty())
			.andExpect(jsonPath("$.role").value("SUPPLIER"))
			.andExpect(jsonPath("$.displayName").value(company))
			.andExpect(jsonPath("$.canActAs").value(false))
			.andReturn()
			.getResponse()
			.getContentAsString();
		String token = JsonPath.read(session, "$.token");
		long supplierId = ((Number) JsonPath.read(session, "$.partyId")).longValue();

		// The token works, and only for the supplier's own data.
		mvc.perform(get("/api/suppliers/{id}", supplierId).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.companyName").value(company));
		mvc.perform(get("/api/suppliers").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
			.andExpect(status().isForbidden());

		// The new account can sign in again (username ignores case).
		login(username.toUpperCase(), "12345678").andExpect(status().isOk())
			.andExpect(jsonPath("$.partyId").value(supplierId));
	}

	@Test
	void retailerSignUpEnforcesTheStoreTypeRules() throws Exception {
		String online = """
				{"username": "%s", "password": "12345678", "role": "RETAILER",
				 "retailer": {"storeName": "%s", "storeType": "ONLINE", "countryCode": "BE",
				              "website": "%s", "contactEmail": "hello@shop.be"}}
				""";
		mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
			.content(online.formatted(unique("kiosk"), unique("Kiosk"), "https://kiosk.example.be")))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.role").value("RETAILER"));

		// BR-02: an online store without a website is refused, and no account is created.
		String username = unique("nosite");
		mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
			.content(online.formatted(username, unique("No Site"), "")))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_LOCATION"));
		login(username, "12345678").andExpect(status().isUnauthorized());
	}

	@Test
	void usernamesAreUniqueAndPasswordsLongEnough() throws Exception {
		String username = unique("taken");
		signUpSupplier(username, "12345678", unique("First Co")).andExpect(status().isCreated());

		signUpSupplier(username.toUpperCase(), "12345678", unique("Second Co"))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
		signUpSupplier(unique("short"), "1234567", unique("Third Co"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void nobodyCanSignUpAsAdministrator() throws Exception {
		mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "%s", "password": "12345678", "role": "ADMIN"}
				""".formatted(unique("boss"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("INVALID_SIGNUP"));
	}

	@Test
	void wrongCredentialsGiveTheSameAnswerForUnknownUsers() throws Exception {
		String username = unique("someone");
		signUpSupplier(username, "12345678", unique("Someone Co")).andExpect(status().isCreated());

		login(username, "wrong-password").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
		login(unique("nobody"), "12345678").andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	void onlyAnAdministratorCanActAsAnotherPersona() throws Exception {
		String admin = unique("admin");
		users.saveAndFlush(UserAccount.administrator(admin, passwordEncoder.encode("admin-password")));
		String adminToken = JsonPath.read(login(admin, "admin-password").andExpect(status().isOk())
			.andExpect(jsonPath("$.canActAs").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.token");

		String supplierSession = signUpSupplier(unique("acme"), "12345678", unique("Acme")).andReturn()
			.getResponse()
			.getContentAsString();
		long supplierId = ((Number) JsonPath.read(supplierSession, "$.partyId")).longValue();
		String supplierToken = JsonPath.read(supplierSession, "$.token");

		// Admin -> supplier: still the admin account, still allowed to switch.
		String actingToken = JsonPath.read(actAs(adminToken, "SUPPLIER:" + supplierId).andExpect(status().isOk())
			.andExpect(jsonPath("$.username").value(admin))
			.andExpect(jsonPath("$.role").value("SUPPLIER"))
			.andExpect(jsonPath("$.partyId").value(supplierId))
			.andExpect(jsonPath("$.canActAs").value(true))
			.andReturn()
			.getResponse()
			.getContentAsString(), "$.token");
		mvc.perform(get("/api/suppliers").header(HttpHeaders.AUTHORIZATION, "Bearer " + actingToken))
			.andExpect(status().isForbidden());
		actAs(actingToken, "ADMIN").andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));

		// A real supplier cannot switch, and unknown companies are refused.
		actAs(supplierToken, "ADMIN").andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN_ACTION"));
		actAs(adminToken, "SUPPLIER:999999").andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("SUPPLIER_NOT_FOUND"));
		mvc.perform(post("/api/auth/act-as").contentType(MediaType.APPLICATION_JSON).content("{\"persona\": \"ADMIN\"}"))
			.andExpect(status().isUnauthorized());
	}

	private ResultActions signUpSupplier(String username, String password, String company) throws Exception {
		return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content("""
				{"username": "%s", "password": "%s", "role": "SUPPLIER",
				 "supplier": {"companyName": "%s", "contactName": "Lukas Meier", "contactEmail": "lukas@example.ch",
				              "contactPhone": "+41 44 210 33 10", "countryCode": "CH"}}
				""".formatted(username, password, company)));
	}

	private ResultActions login(String username, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
			.content("{\"username\": \"%s\", \"password\": \"%s\"}".formatted(username, password)));
	}

	private ResultActions actAs(String token, String persona) throws Exception {
		return mvc.perform(post("/api/auth/act-as").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
			.contentType(MediaType.APPLICATION_JSON)
			.content("{\"persona\": \"%s\"}".formatted(persona)));
	}

	/** Tests share one database, so each one uses its own names. */
	private static String unique(String base) {
		return base.replace(" ", "") + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

}
