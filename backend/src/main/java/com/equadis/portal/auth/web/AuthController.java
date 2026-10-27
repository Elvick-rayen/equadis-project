package com.equadis.portal.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.auth.application.AuthService;
import com.equadis.portal.auth.application.AuthSession;
import com.equadis.portal.auth.application.command.SignUp;
import com.equadis.portal.retailer.application.command.CreateRetailer;
import com.equadis.portal.retailer.domain.Location;
import com.equadis.portal.shared.actor.ActorRole;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.CountryCode;
import com.equadis.portal.shared.domain.Email;
import com.equadis.portal.shared.domain.ErrorCode;
import com.equadis.portal.supplier.application.command.CreateSupplier;
import com.equadis.portal.supplier.domain.ContactInfo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
class AuthController {

	private final AuthService auth;

	AuthController(AuthService auth) {
		this.auth = auth;
	}

	@PostMapping("/login")
	@Operation(summary = "Sign in with username and password; returns a bearer token")
	AuthSession login(@Valid @RequestBody AuthRequests.Login request) {
		return auth.login(request.username(), request.password());
	}

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(summary = "Create a supplier or retailer company with its account, and sign in")
	AuthSession signUp(@Valid @RequestBody AuthRequests.SignUp request) {
		return auth.signUp(toCommand(request));
	}

	@PostMapping("/act-as")
	@Operation(summary = "Administrators only: get a token acting as another persona, without signing out")
	AuthSession actAs(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AuthRequests.ActAs request) {
		return auth.actAs(jwt.getSubject(), CurrentActor.parse(request.persona()));
	}

	private static SignUp toCommand(AuthRequests.SignUp request) {
		if (request.role() == ActorRole.SUPPLIER && request.supplier() != null && request.retailer() == null) {
			AuthRequests.SupplierDetails supplier = request.supplier();
			return SignUp.asSupplier(request.username(), request.password(),
					new CreateSupplier(supplier.companyName(),
							new ContactInfo(supplier.contactName(), supplier.contactEmail(), supplier.contactPhone()),
							CountryCode.of(supplier.countryCode())));
		}
		if (request.role() == ActorRole.RETAILER && request.retailer() != null && request.supplier() == null) {
			AuthRequests.RetailerDetails retailer = request.retailer();
			Location location = Location.forStoreType(retailer.storeType(), CountryCode.of(retailer.countryCode()),
					retailer.city(), retailer.address(), retailer.website());
			return SignUp.asRetailer(request.username(), request.password(),
					new CreateRetailer(retailer.storeName(), retailer.storeType(), location, Email.of(retailer.contactEmail())));
		}
		throw new BusinessException(ErrorCode.INVALID_SIGNUP,
				"Sign up as SUPPLIER with supplier details, or as RETAILER with retailer details");
	}

}
