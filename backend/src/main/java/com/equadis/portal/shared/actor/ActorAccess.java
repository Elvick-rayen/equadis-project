package com.equadis.portal.shared.actor;

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

	private static BusinessException forbidden(CurrentActor actor) {
		return new BusinessException(ErrorCode.FORBIDDEN_ACTION, "Not allowed when acting as " + actor.role());
	}

}
