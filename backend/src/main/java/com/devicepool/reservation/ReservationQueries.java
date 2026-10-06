package com.devicepool.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-side lookups other modules need without depending on the Reservation entity. */
@Service
public class ReservationQueries {

	private final ReservationRepository reservations;

	public ReservationQueries(ReservationRepository reservations) {
		this.reservations = reservations;
	}

	@Transactional(readOnly = true)
	public Map<Long, List<BookingSlot>> bookingsByDevice(Instant from, Instant to) {
		return reservations.findAllOverlapping(from, to).stream()
				.collect(Collectors.groupingBy(r -> r.getDevice().getId(),
						Collectors.mapping(BookingSlot::from, Collectors.toList())));
	}

	/** The people who most recently had the device, up to {@code limit}, as of {@code before}. */
	@Transactional(readOnly = true)
	public List<BookingSlot> recentHolders(Long deviceId, Instant before, int limit) {
		return reservations.findRecentHolders(deviceId, before, limit).stream().map(BookingSlot::from).toList();
	}
}
