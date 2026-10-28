package com.equadis.portal.modules.partnership.infrastructure;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.partnership.PartnershipQueries;
import com.equadis.portal.modules.partnership.PartnershipStatus;
import com.equadis.portal.modules.partnership.domain.Partnership;
import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.supplier.SupplierQueries;
import com.equadis.portal.modules.supplier.SupplierSummary;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Read side of the partnership module: filtered, paged lists with company names, linked
 * companies, and the public {@link PartnershipQueries}. Names come from the supplier and
 * retailer modules' public APIs, never from their tables, so module boundaries hold.
 */
@Component
@Transactional(readOnly = true)
public class PartnershipReadModel implements PartnershipQueries {

	private static final Set<String> SORTABLE = Set.of("startDate", "endDate");

	/** "No upper bound" for the end-date filter; PostgreSQL dates stop at year 5874897, so not LocalDate.MAX. */
	private static final LocalDate NO_END_LIMIT = LocalDate.of(9999, 12, 31);

	private final SpringDataPartnershipRepository repository;

	private final SupplierQueries suppliers;

	private final RetailerQueries retailers;

	private final Clock clock;

	PartnershipReadModel(SpringDataPartnershipRepository repository, SupplierQueries suppliers,
			RetailerQueries retailers, Clock clock) {
		this.repository = repository;
		this.suppliers = suppliers;
		this.retailers = retailers;
		this.clock = clock;
	}

	/** Supplier and retailer names for a set of partnerships, fetched in two queries. */
	public record PartyNames(Map<Long, String> suppliers, Map<Long, String> retailers) {

		public String supplier(long id) {
			return suppliers.getOrDefault(id, "#" + id);
		}

		public String retailer(long id) {
			return retailers.getOrDefault(id, "#" + id);
		}

	}

	/** A company linked to another through an ACTIVE partnership. */
	public record LinkedPartner(long partnershipId, long companyId, String companyName, LocalDate startDate,
			LocalDate endDate) {
	}

	/**
	 * @param status null for all statuses
	 * @param expiringWithinDays when set, only ACTIVE partnerships ending within that many days
	 */
	public Page<Partnership> search(PartnershipStatus status, Integer expiringWithinDays, Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new BusinessException(ErrorCode.INVALID_SORT,
						"Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
			}
		}
		LocalDate today = today();
		String statusFilter = status == null ? "ALL" : status.name();
		LocalDate endsBy = NO_END_LIMIT;
		if (expiringWithinDays != null) {
			if (expiringWithinDays < 0) {
				throw new BusinessException(ErrorCode.VALIDATION_FAILED, "expiringWithinDays must be 0 or more");
			}
			statusFilter = PartnershipStatus.ACTIVE.name();
			endsBy = today.plusDays(expiringWithinDays);
		}
		return repository.search(0, 0, statusFilter, today, endsBy, pageable);
	}

	public PartyNames namesFor(Collection<Partnership> partnerships) {
		Set<Long> supplierIds = partnerships.stream().map(Partnership::supplierId).collect(Collectors.toSet());
		Set<Long> retailerIds = partnerships.stream().map(Partnership::retailerId).collect(Collectors.toSet());
		return new PartyNames(
				suppliers.findSummaries(supplierIds)
					.stream()
					.collect(Collectors.toMap(SupplierSummary::id, SupplierSummary::companyName)),
				retailers.findSummaries(retailerIds)
					.stream()
					.collect(Collectors.toMap(RetailerSummary::id, RetailerSummary::storeName)));
	}

	/** Retailers linked to a supplier: those with an ACTIVE partnership today. */
	public List<LinkedPartner> linkedRetailers(long supplierId) {
		List<Partnership> active = repository.findActiveForSupplier(supplierId, today());
		Map<Long, String> names = retailers.findSummaries(active.stream().map(Partnership::retailerId).toList())
			.stream()
			.collect(Collectors.toMap(RetailerSummary::id, RetailerSummary::storeName));
		return toLinked(active, Partnership::retailerId, names);
	}

	/** Suppliers linked to a retailer: those with an ACTIVE partnership today. */
	public List<LinkedPartner> linkedSuppliers(long retailerId) {
		List<Partnership> active = repository.findActiveForRetailer(retailerId, today());
		Map<Long, String> names = suppliers.findSummaries(active.stream().map(Partnership::supplierId).toList())
			.stream()
			.collect(Collectors.toMap(SupplierSummary::id, SupplierSummary::companyName));
		return toLinked(active, Partnership::supplierId, names);
	}

	@Override
	public boolean hasActivePartnership(long supplierId, long retailerId) {
		return repository.existsActive(supplierId, retailerId, today());
	}

	@Override
	public Set<Long> activeSupplierIds(long retailerId) {
		return repository.findActiveForRetailer(retailerId, today())
			.stream()
			.map(Partnership::supplierId)
			.collect(Collectors.toSet());
	}

	@Override
	public List<ExpiringPartnership> endingOn(LocalDate endDate) {
		return repository.findNotTerminatedEndingOn(endDate)
			.stream()
			.map(p -> new ExpiringPartnership(p.id(), p.supplierId(), p.retailerId(), p.endDate()))
			.toList();
	}

	/** Today in Europe/Paris; every status shown is relative to this day (BR-05). */
	public LocalDate today() {
		return LocalDate.now(clock);
	}

	private static List<LinkedPartner> toLinked(List<Partnership> partnerships, Function<Partnership, Long> companyId,
			Map<Long, String> names) {
		return partnerships.stream()
			.map(p -> new LinkedPartner(p.id(), companyId.apply(p), names.getOrDefault(companyId.apply(p), ""),
					p.startDate(), p.endDate()))
			.sorted(Comparator.comparing(LinkedPartner::companyName, String.CASE_INSENSITIVE_ORDER))
			.toList();
	}

}
