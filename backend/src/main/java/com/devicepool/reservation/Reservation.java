package com.devicepool.reservation;

import java.time.Instant;

import com.devicepool.device.Device;
import com.devicepool.user.AppUser;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** A device booked by a user for the half-open interval [startAt, endAt). */
@Entity
public class Reservation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "device_id")
	private Device device;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id")
	private AppUser user;

	private Instant startAt;

	private Instant endAt;

	private Instant createdAt;

	private Instant cancelledAt;

	protected Reservation() {
	}

	public Reservation(Device device, AppUser user, Instant startAt, Instant endAt, Instant createdAt) {
		this.device = device;
		this.user = user;
		this.startAt = startAt;
		this.endAt = endAt;
		this.createdAt = createdAt;
	}

	public ReservationStatus statusAt(Instant now) {
		if (cancelledAt != null) {
			return ReservationStatus.CANCELLED;
		}
		if (!endAt.isAfter(now)) {
			return ReservationStatus.FINISHED;
		}
		return startAt.isAfter(now) ? ReservationStatus.UPCOMING : ReservationStatus.ACTIVE;
	}

	void cancel(Instant now) {
		this.cancelledAt = now;
	}

	/** Ends a running reservation early, e.g. when the device is brought back before the slot is over. */
	void endEarly(Instant now) {
		this.endAt = now;
	}

	public Long getId() {
		return id;
	}

	public Device getDevice() {
		return device;
	}

	public AppUser getUser() {
		return user;
	}

	public Instant getStartAt() {
		return startAt;
	}

	public Instant getEndAt() {
		return endAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getCancelledAt() {
		return cancelledAt;
	}
}
