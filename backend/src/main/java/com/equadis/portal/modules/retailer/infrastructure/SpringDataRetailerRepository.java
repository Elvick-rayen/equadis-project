package com.equadis.portal.modules.retailer.infrastructure;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.modules.retailer.RetailerSummary;
import com.equadis.portal.modules.retailer.domain.Retailer;
import com.equadis.portal.modules.retailer.domain.RetailerRepository;

/** Spring Data adapter: implements the domain port and adds read-side queries. */
interface SpringDataRetailerRepository extends JpaRepository<Retailer, Long>, RetailerRepository {

	@Query("select new com.equadis.portal.modules.retailer.RetailerSummary(r.id, r.storeName) from Retailer r order by lower(r.storeName)")
	List<RetailerSummary> findAllSummaries();

	@Query("select new com.equadis.portal.modules.retailer.RetailerSummary(r.id, r.storeName) from Retailer r where r.id = :id")
	Optional<RetailerSummary> findSummary(@Param("id") long id);

	@Query("select new com.equadis.portal.modules.retailer.RetailerSummary(r.id, r.storeName) from Retailer r where r.id in :ids order by lower(r.storeName)")
	List<RetailerSummary> findSummaries(@Param("ids") Collection<Long> ids);

	@Query("select new com.equadis.portal.modules.retailer.RetailerSummary(r.id, r.storeName) from Retailer r where lower(r.storeName) = lower(:name)")
	List<RetailerSummary> findByStoreNameIgnoreCase(@Param("name") String name);

	@Query("""
			select r from Retailer r
			where lower(r.storeName) like :pattern
			   or lower(r.contactEmail) like :pattern
			   or lower(r.location.city) like :pattern
			   or lower(r.location.countryCode) like :pattern
			""")
	Page<Retailer> search(@Param("pattern") String pattern, Pageable pageable);

}
