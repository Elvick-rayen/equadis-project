package com.equadis.portal.retailer.infrastructure;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.retailer.RetailerSummary;
import com.equadis.portal.retailer.domain.Retailer;
import com.equadis.portal.retailer.domain.RetailerRepository;

/** Spring Data adapter: implements the domain port and adds read-side queries. */
interface SpringDataRetailerRepository extends JpaRepository<Retailer, Long>, RetailerRepository {

	@Query("select new com.equadis.portal.retailer.RetailerSummary(r.id, r.storeName) from Retailer r order by lower(r.storeName)")
	List<RetailerSummary> findAllSummaries();

	@Query("select new com.equadis.portal.retailer.RetailerSummary(r.id, r.storeName) from Retailer r where r.id = :id")
	Optional<RetailerSummary> findSummary(@Param("id") long id);

	@Query("select new com.equadis.portal.retailer.RetailerSummary(r.id, r.storeName) from Retailer r where lower(r.storeName) = lower(:name)")
	List<RetailerSummary> findByStoreNameIgnoreCase(@Param("name") String name);

}
