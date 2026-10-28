package com.equadis.portal.modules.auth.application;

import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.auth.domain.UserAccount;
import com.equadis.portal.modules.auth.domain.UserAccountRepository;
import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;

/**
 * Dev profile only: creates the three demo accounts if they are missing.
 * In code rather than SQL so passwords are hashed by the app's own PasswordEncoder.
 * <ul>
 * <li>admin / admin</li>
 * <li>supplier / 12345678, for the seeded supplier "Alpina Foods"</li>
 * <li>retailer / 12345678, for the seeded retailer "City Market Lyon"</li>
 * </ul>
 */
@Component
@Profile("dev")
class DemoAccountsSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DemoAccountsSeeder.class);

	private static final String DEMO_PASSWORD = "12345678";

	private final UserAccountRepository users;

	private final PasswordEncoder passwordEncoder;

	private final SupplierQueries suppliers;

	private final RetailerQueries retailers;

	DemoAccountsSeeder(UserAccountRepository users, PasswordEncoder passwordEncoder, SupplierQueries suppliers,
			RetailerQueries retailers) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.suppliers = suppliers;
		this.retailers = retailers;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		createIfMissing("admin", () -> Optional.of(UserAccount.administrator("admin", passwordEncoder.encode("admin"))));
		createIfMissing("supplier", () -> suppliers.findByCompanyName("Alpina Foods")
			.map(SupplierSummary::id)
			.map(id -> UserAccount.forSupplier("supplier", passwordEncoder.encode(DEMO_PASSWORD), id)));
		createIfMissing("retailer", () -> retailers.findByStoreName("City Market Lyon")
			.map(RetailerSummary::id)
			.map(id -> UserAccount.forRetailer("retailer", passwordEncoder.encode(DEMO_PASSWORD), id)));
	}

	private void createIfMissing(String username, Supplier<Optional<UserAccount>> account) {
		if (users.existsByUsernameIgnoreCase(username)) {
			return;
		}
		account.get().ifPresentOrElse(created -> {
			users.saveAndFlush(created);
			log.info("Created demo account '{}'", username);
		}, () -> log.warn("Demo account '{}' not created: its company is missing from the seed data", username));
	}

}
