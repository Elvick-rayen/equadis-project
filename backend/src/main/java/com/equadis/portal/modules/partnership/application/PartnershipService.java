package com.equadis.portal.modules.partnership.application;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.partnership.PartnershipCreated;
import com.equadis.portal.modules.partnership.PartnershipRenewed;
import com.equadis.portal.modules.partnership.PartnershipTerminated;
import com.equadis.portal.modules.partnership.application.command.CreatePartnership;
import com.equadis.portal.modules.partnership.application.command.RenewPartnership;
import com.equadis.portal.modules.partnership.application.command.TerminatePartnership;
import com.equadis.portal.modules.partnership.domain.OverlapPolicy;
import com.equadis.portal.modules.partnership.domain.Partnership;
import com.equadis.portal.modules.partnership.domain.PartnershipRepository;
import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.DateRange;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Partnership use cases. Rules on one partnership live in {@link Partnership}; this service adds
 * what needs other data: both companies exist, no overlap (BR-04), and it publishes events so
 * other modules can react (subscriptions are cancelled on termination, the audit log records).
 */
@Service
@Transactional
public class PartnershipService {

	private final PartnershipRepository partnerships;

	private final OverlapPolicy overlapPolicy;

	private final SupplierQueries suppliers;

	private final RetailerQueries retailers;

	private final ApplicationEventPublisher events;

	private final Clock clock;

	public PartnershipService(PartnershipRepository partnerships, SupplierQueries suppliers, RetailerQueries retailers,
			ApplicationEventPublisher events, Clock clock) {
		this.partnerships = partnerships;
		this.overlapPolicy = new OverlapPolicy(partnerships);
		this.suppliers = suppliers;
		this.retailers = retailers;
		this.events = events;
		this.clock = clock;
	}

	public Partnership create(CreatePartnership command) {
		if (!suppliers.exists(command.supplierId())) {
			throw new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND, "Supplier " + command.supplierId() + " not found");
		}
		if (!retailers.exists(command.retailerId())) {
			throw new BusinessException(ErrorCode.RETAILER_NOT_FOUND, "Retailer " + command.retailerId() + " not found");
		}
		Partnership partnership = Partnership.create(command.supplierId(), command.retailerId(), command.period(), today());
		overlapPolicy.requireNoOverlap(command.supplierId(), command.retailerId(), command.period());

		Partnership saved = save(partnership);
		events.publishEvent(new PartnershipCreated(saved.id(), saved.supplierId(), saved.retailerId(), saved.startDate(),
				saved.endDate()));
		return saved;
	}

	public Partnership renew(RenewPartnership command) {
		Partnership partnership = get(command.partnershipId());
		LocalDate previousEndDate = partnership.endDate();
		LocalDate today = today();
		// Check everything before changing the entity: the overlap query would otherwise flush
		// the change first, and the database constraint would fire instead of a clear error.
		partnership.checkRenewal(command.newEndDate(), today);
		overlapPolicy.requireNoOverlap(partnership, DateRange.of(partnership.startDate(), command.newEndDate()));
		partnership.renew(command.newEndDate(), today);

		Partnership saved = save(partnership);
		events.publishEvent(new PartnershipRenewed(saved.id(), saved.supplierId(), saved.retailerId(), previousEndDate,
				saved.endDate()));
		return saved;
	}

	public Partnership terminate(TerminatePartnership command) {
		Partnership partnership = get(command.partnershipId());
		partnership.terminate(command.reason(), today());

		Partnership saved = save(partnership);
		// Listeners run now, in this transaction: if cancelling subscriptions fails, nothing is terminated.
		events.publishEvent(new PartnershipTerminated(saved.id(), saved.supplierId(), saved.retailerId(),
				saved.terminationReason(), saved.terminatedOn()));
		return saved;
	}

	@Transactional(readOnly = true)
	public Partnership get(long partnershipId) {
		return partnerships.findById(partnershipId)
			.orElseThrow(() -> new BusinessException(ErrorCode.PARTNERSHIP_NOT_FOUND,
					"Partnership " + partnershipId + " not found"));
	}

	/** Today in Europe/Paris (BR-05: every status is relative to this). */
	public LocalDate today() {
		return LocalDate.now(clock);
	}

	/** The exclusion constraint is the last guard against two requests racing past BR-04. */
	private Partnership save(Partnership partnership) {
		try {
			return partnerships.saveAndFlush(partnership);
		}
		catch (DataIntegrityViolationException ex) {
			throw new BusinessException(ErrorCode.PARTNERSHIP_OVERLAP,
					"This supplier and retailer already have an overlapping partnership");
		}
	}

}
