package com.equadis.portal.supplier.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;
import com.equadis.portal.supplier.SupplierQueries;
import com.equadis.portal.supplier.SupplierSummary;
import com.equadis.portal.supplier.domain.Supplier;

/** Read side: search with pagination, and the public {@link SupplierQueries}. */
@Component
@Transactional(readOnly = true)
public class SupplierReadModel implements SupplierQueries {

	/** Properties clients may sort on. Anything else is rejected with INVALID_SORT. */
	private static final Set<String> SORTABLE = Set.of("companyName", "contact.name", "contact.email", "countryCode");

	private final SpringDataSupplierRepository repository;

	SupplierReadModel(SpringDataSupplierRepository repository) {
		this.repository = repository;
	}

	/** Case-insensitive "contains" search on name, contact and country. A blank query returns everything. */
	public Page<Supplier> search(String query, Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new BusinessException(ErrorCode.INVALID_SORT,
						"Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
			}
		}
		return repository.search(likePattern(query), pageable);
	}

	@Override
	public boolean exists(long supplierId) {
		return repository.existsById(supplierId);
	}

	@Override
	public List<SupplierSummary> findAllSummaries() {
		return repository.findAllSummaries();
	}

	@Override
	public List<SupplierSummary> findSummaries(Collection<Long> supplierIds) {
		return supplierIds.isEmpty() ? List.of() : repository.findSummaries(supplierIds);
	}

	private static String likePattern(String query) {
		if (query == null || query.isBlank()) {
			return "%";
		}
		String escaped = query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
