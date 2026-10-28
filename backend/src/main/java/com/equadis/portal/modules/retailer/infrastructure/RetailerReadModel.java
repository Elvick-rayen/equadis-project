package com.equadis.portal.modules.retailer.infrastructure;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.retailer.RetailerQueries;
import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.retailer.domain.Retailer;
import com.equadis.portal.shared.domain.BusinessException;
import com.equadis.portal.shared.domain.ErrorCode;

/**
 * Read side of the retailer module: search with pagination for the retailers screen,
 * and the public {@link RetailerQueries} for other modules.
 */
@Component
@Transactional(readOnly = true)
public class RetailerReadModel implements RetailerQueries {

	/** Properties clients may sort on. Anything else is rejected with INVALID_SORT. */
	private static final Set<String> SORTABLE = Set.of("storeName", "storeType", "contactEmail", "location.city",
			"location.countryCode");

	private final SpringDataRetailerRepository repository;

	RetailerReadModel(SpringDataRetailerRepository repository) {
		this.repository = repository;
	}

	/** Case-insensitive "contains" search on store name, e-mail, city and country. A blank query returns everything. */
	public Page<Retailer> search(String query, Pageable pageable) {
		for (Sort.Order order : pageable.getSort()) {
			if (!SORTABLE.contains(order.getProperty())) {
				throw new BusinessException(ErrorCode.INVALID_SORT,
						"Cannot sort by '" + order.getProperty() + "'. Allowed: " + SORTABLE);
			}
		}
		return repository.search(likePattern(query), pageable);
	}

	@Override
	public Optional<RetailerSummary> findSummary(long retailerId) {
		return repository.findSummary(retailerId);
	}

	@Override
	public List<RetailerSummary> findAllSummaries() {
		return repository.findAllSummaries();
	}

	@Override
	public Optional<RetailerSummary> findByStoreName(String storeName) {
		return repository.findByStoreNameIgnoreCase(storeName).stream().findFirst();
	}

	private static String likePattern(String query) {
		if (query == null || query.isBlank()) {
			return "%";
		}
		String escaped = query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
		return "%" + escaped + "%";
	}

}
