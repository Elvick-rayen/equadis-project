package com.equadis.portal.modules.partnership.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.equadis.portal.modules.partnership.domain.Partnership;
import com.equadis.portal.modules.partnership.domain.PartnershipRepository;

/**
 * Spring Data adapter: implements the domain port and adds read-side queries. Status is not a
 * column (BR-05), so each status filter is the same date condition the domain uses, relative to
 * {@code :today}.
 */
interface SpringDataPartnershipRepository extends JpaRepository<Partnership, Long>, PartnershipRepository {

	/** Loads the renewal history with the partnership: callers map it after the transaction ends. */
	@Override
	@EntityGraph(attributePaths = "renewals")
	Optional<Partnership> findById(Long id);

	@Override
	@Query("""
			select count(p) > 0 from Partnership p
			where p.supplierId = :supplierId and p.retailerId = :retailerId
			  and p.terminatedOn is null
			  and p.startDate <= :end and p.endDate >= :start
			  and p.id <> :excludedId
			""")
	boolean existsOverlapping(@Param("supplierId") long supplierId, @Param("retailerId") long retailerId,
			@Param("start") LocalDate start, @Param("end") LocalDate end, @Param("excludedId") long excludedId);

	/** {@code status}: ALL, SCHEDULED, ACTIVE, EXPIRED or TERMINATED. {@code endsBy} caps the end date. */
	@Query("""
			select p from Partnership p
			where (:supplierId = 0 or p.supplierId = :supplierId)
			  and (:retailerId = 0 or p.retailerId = :retailerId)
			  and (:status = 'ALL'
			    or (:status = 'TERMINATED' and p.terminatedOn is not null)
			    or (:status = 'SCHEDULED' and p.terminatedOn is null and p.startDate > :today)
			    or (:status = 'ACTIVE' and p.terminatedOn is null and p.startDate <= :today and p.endDate >= :today)
			    or (:status = 'EXPIRED' and p.terminatedOn is null and p.endDate < :today))
			  and p.endDate <= :endsBy
			""")
	Page<Partnership> search(@Param("supplierId") long supplierId, @Param("retailerId") long retailerId,
			@Param("status") String status, @Param("today") LocalDate today, @Param("endsBy") LocalDate endsBy,
			Pageable pageable);

	@Query("""
			select count(p) > 0 from Partnership p
			where p.supplierId = :supplierId and p.retailerId = :retailerId
			  and p.terminatedOn is null and p.startDate <= :today and p.endDate >= :today
			""")
	boolean existsActive(@Param("supplierId") long supplierId, @Param("retailerId") long retailerId,
			@Param("today") LocalDate today);

	@Query("""
			select p from Partnership p
			where p.retailerId = :retailerId
			  and p.terminatedOn is null and p.startDate <= :today and p.endDate >= :today
			""")
	List<Partnership> findActiveForRetailer(@Param("retailerId") long retailerId, @Param("today") LocalDate today);

	@Query("""
			select p from Partnership p
			where p.supplierId = :supplierId
			  and p.terminatedOn is null and p.startDate <= :today and p.endDate >= :today
			""")
	List<Partnership> findActiveForSupplier(@Param("supplierId") long supplierId, @Param("today") LocalDate today);

	@Query("select p from Partnership p where p.terminatedOn is null and p.endDate = :endDate")
	List<Partnership> findNotTerminatedEndingOn(@Param("endDate") LocalDate endDate);

}
