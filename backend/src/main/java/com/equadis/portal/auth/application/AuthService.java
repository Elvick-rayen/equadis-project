package com.equadis.portal.auth.application;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.auth.application.command.SignUp;
import com.equadis.portal.auth.domain.UserAccount;
import com.equadis.portal.auth.domain.UserAccountRepository;
import com.equadis.portal.retailer.RetailerQueries;
import com.equadis.portal.retailer.RetailerSummary;
import com.equadis.portal.retailer.application.RetailerService;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;
import com.equadis.portal.supplier.SupplierQueries;
import com.equadis.portal.supplier.SupplierSummary;
import com.equadis.portal.supplier.application.SupplierService;

/** Sign in, sign up and (for administrators) switch persona. Every path ends in a fresh token. */
@Service
@Transactional
public class AuthService {

	public static final int MIN_PASSWORD_LENGTH = 8;

	/** bcrypt only uses the first 72 bytes. */
	public static final int MAX_PASSWORD_LENGTH = 72;

	private final UserAccountRepository users;

	private final PasswordEncoder passwordEncoder;

	private final TokenService tokens;

	private final SupplierService supplierService;

	private final RetailerService retailerService;

	private final SupplierQueries supplierQueries;

	private final RetailerQueries retailerQueries;

	/** Compared against when the username is unknown, so both failures take the same time. */
	private final String unknownUserHash;

	AuthService(UserAccountRepository users, PasswordEncoder passwordEncoder, TokenService tokens,
			SupplierService supplierService, RetailerService retailerService, SupplierQueries supplierQueries,
			RetailerQueries retailerQueries) {
		this.users = users;
		this.passwordEncoder = passwordEncoder;
		this.tokens = tokens;
		this.supplierService = supplierService;
		this.retailerService = retailerService;
		this.supplierQueries = supplierQueries;
		this.retailerQueries = retailerQueries;
		this.unknownUserHash = passwordEncoder.encode("unknown-user-timing-guard");
	}

	@Transactional(readOnly = true)
	public AuthSession login(String username, String password) {
		UserAccount account = users.findByUsernameIgnoreCase(username == null ? "" : username.trim()).orElse(null);
		String hash = account == null ? unknownUserHash : account.passwordHash();
		if (!passwordEncoder.matches(password == null ? "" : password, hash) || account == null) {
			// Same answer for an unknown user and a wrong password.
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Wrong username or password");
		}
		return sessionFor(account.username(), account.actor());
	}

	/** Creates the company and its account in one transaction, then signs the new user in. */
	public AuthSession signUp(SignUp command) {
		String username = UserAccount.requireValidUsername(command.username());
		requireValidPassword(command.password());
		if ((command.supplier() == null) == (command.retailer() == null)) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP, "Sign up as either a supplier or a retailer");
		}
		if (users.existsByUsernameIgnoreCase(username)) {
			throw usernameTaken(username);
		}

		String hash = passwordEncoder.encode(command.password());
		UserAccount account = command.supplier() != null
				? UserAccount.forSupplier(username, hash, supplierService.create(command.supplier()).id())
				: UserAccount.forRetailer(username, hash, retailerService.create(command.retailer()).id());
		try {
			users.saveAndFlush(account);
		}
		catch (DataIntegrityViolationException ex) {
			throw usernameTaken(username);
		}
		return sessionFor(account.username(), account.actor());
	}

	/**
	 * Administrator-only: a token for another persona, keeping the administrator signed in
	 * (the subject stays the admin's username and {@code canActAs} stays true).
	 * The admin role is checked against the account, not taken from the caller's token.
	 */
	@Transactional(readOnly = true)
	public AuthSession actAs(String username, CurrentActor target) {
		if (!isAdministrator(username)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "Only administrators can act as another persona");
		}
		return sessionFor(username, target);
	}

	private boolean isAdministrator(String username) {
		return users.findByUsernameIgnoreCase(username).map(UserAccount::actor).filter(CurrentActor::isAdmin).isPresent();
	}

	private AuthSession sessionFor(String username, CurrentActor actor) {
		String displayName = switch (actor.role()) {
			case ADMIN -> null;
			case SUPPLIER -> supplierQueries.findSummary(actor.id())
				.map(SupplierSummary::companyName)
				.orElseThrow(() -> new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier " + actor.id() + " not found"));
			case RETAILER -> retailerQueries.findSummary(actor.id())
				.map(RetailerSummary::storeName)
				.orElseThrow(() -> new BusinessException(ErrorCode.RETAILER_NOT_FOUND, "Retailer " + actor.id() + " not found"));
		};
		return tokens.issue(username, actor, displayName, isAdministrator(username));
	}

	private static void requireValidPassword(String password) {
		if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
			throw new BusinessException(ErrorCode.INVALID_SIGNUP,
					"Password must be " + MIN_PASSWORD_LENGTH + " to " + MAX_PASSWORD_LENGTH + " characters");
		}
	}

	private static BusinessException usernameTaken(String username) {
		return new BusinessException(ErrorCode.USERNAME_TAKEN, "Username '" + username + "' is already taken");
	}

}
