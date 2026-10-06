package com.devicepool.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

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

	/** Loads the device and user too, so callers can read them after the transaction ends. */
	@Query("select r from Reservation r join fetch r.device join fetch r.user where r.id = :id")
	Optional<Reservation> findWithDetailsById(@Param("id") Long id);

	/** Non-cancelled reservations of a device that had started by the given time, newest first. */
	@Query("""
			select r from Reservation r join fetch r.user
			where r.device.id = :deviceId
			  and r.cancelledAt is null
			  and r.startAt <= :before
			order by r.startAt desc
			limit :limit
			""")
	List<Reservation> findRecentHolders(@Param("deviceId") Long deviceId,
			@Param("before") Instant before, @Param("limit") int limit);

	@Query("""
			select r from Reservation r join fetch r.device
			where r.user.id = :userId
			order by r.startAt desc
			""")
	List<Reservation> findByUser(@Param("userId") Long userId);
}
