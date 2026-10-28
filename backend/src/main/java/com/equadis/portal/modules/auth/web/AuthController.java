package com.equadis.portal.modules.auth.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.modules.auth.application.AuthService;
import com.equadis.portal.modules.auth.application.AuthSession;
import com.equadis.portal.modules.auth.application.command.SignUp;
import com.equadis.portal.modules.retailer.NewRetailer;
import com.equadis.portal.modules.supplier.NewSupplier;
import com.equadis.portal.shared.actor.ActorRole;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

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
			return SignUp.asSupplier(request.username(), request.password(), new NewSupplier(supplier.companyName(),
					supplier.contactName(), supplier.contactEmail(), supplier.contactPhone(), supplier.countryCode()));
		}
		if (request.role() == ActorRole.RETAILER && request.retailer() != null && request.supplier() == null) {
			AuthRequests.RetailerDetails retailer = request.retailer();
			return SignUp.asRetailer(request.username(), request.password(),
					new NewRetailer(retailer.storeName(), retailer.storeType(), retailer.countryCode(), retailer.city(),
							retailer.address(), retailer.website(), retailer.contactEmail()));
		}
		throw new BusinessException(ErrorCode.INVALID_SIGNUP,
				"Sign up as SUPPLIER with supplier details, or as RETAILER with retailer details");
	}

}
