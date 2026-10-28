package com.equadis.portal.shared.actor;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/** Access checks for the current persona. Stands in for real authorization until roles exist. */
@Component
public class ActorAccess {

	public void requireAdmin(CurrentActor actor) {
		if (!actor.isAdmin()) {
			throw forbidden(actor);
		}
	}

	/**
	 * An administrator account, whichever persona it currently acts as. For the lists the
	 * "Acting as" selector needs, so an administrator acting as a supplier can switch again.
	 */
	public void requireAdministratorAccount(Jwt token) {
		if (!TokenClaims.canActAs(token)) {
			throw new BusinessException(ErrorCode.FORBIDDEN_ACTION, "Only administrator accounts can list every company");
		}
	}

	/** The administrator, or the supplier itself. */
	public void requireAdminOrSupplier(CurrentActor actor, long supplierId) {
		if (!actor.isAdmin() && !actor.isSupplier(supplierId)) {
			throw forbidden(actor);
		}
	}

	/** The administrator, or the retailer itself. */
	public void requireAdminOrRetailer(CurrentActor actor, long retailerId) {
		if (!actor.isAdmin() && !actor.isRetailer(retailerId)) {
			throw forbidden(actor);
		}
	}

	/** The administrator, or either company of a partnership-like pair. */
	public void requireAdminOrParty(CurrentActor actor, long supplierId, long retailerId) {
		if (!actor.isAdmin() && !actor.isSupplier(supplierId) && !actor.isRetailer(retailerId)) {
			throw forbidden(actor);
		}
	}

	private static BusinessException forbidden(CurrentActor actor) {
		return new BusinessException(ErrorCode.FORBIDDEN_ACTION, "Not allowed when acting as " + actor.role());
	}

}
