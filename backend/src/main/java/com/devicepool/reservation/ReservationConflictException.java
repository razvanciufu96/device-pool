package com.devicepool.reservation;

import java.util.List;

public class ReservationConflictException extends RuntimeException {

	private final List<BookingSlot> conflicts;

	public ReservationConflictException(String message, List<BookingSlot> conflicts) {
		super(message);
		this.conflicts = conflicts;
	}

	public List<BookingSlot> getConflicts() {
		return conflicts;
	}
}
