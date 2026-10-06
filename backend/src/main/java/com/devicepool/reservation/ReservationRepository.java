package com.devicepool.reservation;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

	/**
	 * Non-cancelled reservations of one device that overlap [from, to).
	 * Two half-open intervals overlap iff each starts before the other ends.
	 */
	@Query("""
			select r from Reservation r join fetch r.user
			where r.device.id = :deviceId
			  and r.cancelledAt is null
			  and r.startAt < :to
			  and r.endAt > :from
			order by r.startAt
			""")
	List<Reservation> findOverlapping(@Param("deviceId") Long deviceId,
			@Param("from") Instant from, @Param("to") Instant to);

	/** Same as {@link #findOverlapping} but for every device at once (used by the device list). */
	@Query("""
			select r from Reservation r join fetch r.user join fetch r.device
			where r.cancelledAt is null
			  and r.startAt < :to
			  and r.endAt > :from
			order by r.startAt
			""")
	List<Reservation> findAllOverlapping(@Param("from") Instant from, @Param("to") Instant to);

	@Query("""
			select r from Reservation r join fetch r.device
			where r.user.id = :userId
			order by r.startAt desc
			""")
	List<Reservation> findByUser(@Param("userId") Long userId);
}
