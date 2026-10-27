package com.equadis.portal.retailer.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.retailer.RetailerQueries;
import com.equadis.portal.retailer.RetailerSummary;

/** Read side of the retailer module, exposed to other modules as {@link RetailerQueries}. */
@Component
@Transactional(readOnly = true)
public class RetailerReadModel implements RetailerQueries {

	private final SpringDataRetailerRepository repository;

	RetailerReadModel(SpringDataRetailerRepository repository) {
		this.repository = repository;
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

}
