package com.devicepool.reservation;

import java.time.Instant;

/** Who holds a device and when. Shown in the device list and in conflict errors. */
public record BookingSlot(Long reservationId, String userName, Instant start, Instant end) {

	static BookingSlot from(Reservation r) {
		return new BookingSlot(r.getId(), r.getUser().getName(), r.getStartAt(), r.getEndAt());
	}
}
