package com.equadis.portal.modules.audit.application;

import java.time.Clock;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.equadis.portal.modules.audit.domain.AuditEntry;
import com.equadis.portal.modules.audit.domain.AuditRepository;
import com.equadis.portal.modules.audit.domain.AuditSubject;
import com.equadis.portal.modules.offer.OfferClosed;
import com.equadis.portal.modules.offer.OfferCreated;
import com.equadis.portal.modules.offer.OfferEdited;
import com.equadis.portal.modules.offer.OfferPublished;
import com.equadis.portal.modules.partnership.PartnershipCreated;
import com.equadis.portal.modules.partnership.PartnershipRenewed;
import com.equadis.portal.modules.partnership.PartnershipTerminated;
import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;
import com.equadis.portal.shared.actor.ActorRole;
import com.equadis.portal.shared.actor.CurrentActor;
import com.equadis.portal.shared.actor.TokenClaims;

/**
 * Records partnership and offer actions. Runs after the action's transaction commits, so a
 * failed action is never logged, and in its own transaction, so a logging problem cannot undo
 * the action. Same thread as the request, so the signed-in user is still known.
 */
@Component
class AuditListener {

	private final AuditRepository audit;

	private final SupplierQueries suppliers;

	private final RetailerQueries retailers;

	private final Clock clock;

	AuditListener(AuditRepository audit, SupplierQueries suppliers, RetailerQueries retailers, Clock clock) {
		this.audit = audit;
		this.suppliers = suppliers;
		this.retailers = retailers;
		this.clock = clock;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(PartnershipCreated event) {
		record("PARTNERSHIP_CREATED", AuditSubject.PARTNERSHIP, event.partnershipId(),
				"Supplier %s, retailer %s, %s to %s".formatted(supplierName(event.supplierId()),
						retailerName(event.retailerId()), event.startDate(), event.endDate()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(PartnershipRenewed event) {
		record("PARTNERSHIP_RENEWED", AuditSubject.PARTNERSHIP, event.partnershipId(),
				"End date %s → %s".formatted(event.previousEndDate(), event.newEndDate()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(PartnershipTerminated event) {
		record("PARTNERSHIP_TERMINATED", AuditSubject.PARTNERSHIP, event.partnershipId(), "Reason: " + event.reason());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(OfferCreated event) {
		record("OFFER_CREATED", AuditSubject.OFFER, event.offerId(), offer(event.supplierId(), event.reference()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(OfferEdited event) {
		record("OFFER_EDITED", AuditSubject.OFFER, event.offerId(), offer(event.supplierId(), event.reference()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(OfferPublished event) {
		record("OFFER_PUBLISHED", AuditSubject.OFFER, event.offerId(), offer(event.supplierId(), event.reference()));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	void on(OfferClosed event) {
		record("OFFER_CLOSED", AuditSubject.OFFER, event.offerId(), offer(event.supplierId(), event.reference()));
	}

	private String offer(long supplierId, String reference) {
		return "Supplier %s, reference %s".formatted(supplierName(supplierId), reference);
	}

	private void record(String action, AuditSubject subject, long subjectId, String details) {
		audit.save(AuditEntry.record(clock.instant(), currentActor(), action, subject, subjectId, details));
	}

	/** "admin", or "admin (as Alpina Foods)" while an administrator acts as another persona. */
	private String currentActor() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (!(authentication instanceof JwtAuthenticationToken token)) {
			return "system";
		}
		String username = token.getName();
		CurrentActor actor = TokenClaims.actorOf(token.getToken());
		boolean actingAsAnother = TokenClaims.canActAs(token.getToken()) && !actor.isAdmin();
		return actingAsAnother ? "%s (as %s)".formatted(username, companyName(actor)) : username;
	}

	private String companyName(CurrentActor actor) {
		return actor.role() == ActorRole.SUPPLIER ? supplierName(actor.id()) : retailerName(actor.id());
	}

	// Names are stored as they are today, so the log still reads the same after a later rename.
	private String supplierName(long supplierId) {
		return suppliers.findSummary(supplierId).map(SupplierSummary::companyName).orElse("#" + supplierId);
	}

	private String retailerName(long retailerId) {
		return retailers.findSummary(retailerId).map(RetailerSummary::storeName).orElse("#" + retailerId);
	}

}
