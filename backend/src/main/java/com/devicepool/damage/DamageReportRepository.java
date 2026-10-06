package com.devicepool.damage;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DamageReportRepository extends JpaRepository<DamageReport, Long> {

	@Query("""
			select d from DamageReport d
			join fetch d.device join fetch d.reportedBy left join fetch d.resolvedBy
			order by d.reportedAt desc
			""")
	List<DamageReport> findAllWithDetails();

	@Query("""
			select d from DamageReport d
			join fetch d.device join fetch d.reportedBy left join fetch d.resolvedBy
			where d.id = :id
			""")
	Optional<DamageReport> findWithDetailsById(@Param("id") Long id);

	boolean existsByDeviceIdAndResolvedAtIsNull(Long deviceId);
}
