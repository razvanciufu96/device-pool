package com.devicepool.reservation;

/** Derived from the timestamps, never stored, so it can't drift out of sync with them. */
public enum ReservationStatus {
	UPCOMING,
	ACTIVE,
	FINISHED,
	CANCELLED
}
