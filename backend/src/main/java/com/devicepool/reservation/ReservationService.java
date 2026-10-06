package com.devicepool.reservation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devicepool.common.BadRequestException;
import com.devicepool.common.ForbiddenException;
import com.devicepool.common.NotFoundException;
import com.devicepool.device.Device;
import com.devicepool.device.DeviceRepository;
import com.devicepool.user.AppUser;

@Service
public class ReservationService {

	/** Tolerates the few seconds between filling in the form ("now") and submitting it. */
	static final Duration START_GRACE = Duration.ofMinutes(5);

	/** Keeps people from blocking a device indefinitely. */
	static final Duration MAX_DURATION = Duration.ofDays(14);

	private final ReservationRepository reservations;
	private final DeviceRepository devices;
	private final Clock clock;

	public ReservationService(ReservationRepository reservations, DeviceRepository devices, Clock clock) {
		this.reservations = reservations;
		this.devices = devices;
		this.clock = clock;
	}

	/**
	 * Books a device, guaranteeing no overlap with existing reservations.
	 *
	 * The device row is locked first (SELECT ... FOR UPDATE), so concurrent bookings of the
	 * same device run one after another: check-then-insert can't interleave. Bookings of
	 * different devices don't block each other.
	 */
	@Transactional
	public Reservation create(AppUser user, Long deviceId, Instant start, Instant end) {
		Instant now = now();
		validateRange(start, end, now);

		Device device = devices.findByIdForUpdate(deviceId)
				.orElseThrow(() -> new NotFoundException("Device " + deviceId + " not found"));
		if (device.isDamaged()) {
			throw new BadRequestException(device.getName() + " is reported as damaged and can't be booked");
		}

		List<Reservation> overlapping = reservations.findOverlapping(deviceId, start, end);
		if (!overlapping.isEmpty()) {
			throw new ReservationConflictException(
					device.getName() + " is already reserved in that time range",
					overlapping.stream().map(BookingSlot::from).toList());
		}
		return reservations.save(new Reservation(device, user, start, end, now));
	}

	/**
	 * Cancels a future reservation, or ends a running one now (device returned early).
	 * Rows are never deleted so the history of who had which device is preserved.
	 */
	@Transactional
	public Reservation cancel(AppUser user, Long reservationId) {
		Reservation r = reservations.findWithDetailsById(reservationId)
				.orElseThrow(() -> new NotFoundException("Reservation " + reservationId + " not found"));
		if (!r.getUser().getId().equals(user.getId())) {
			throw new ForbiddenException("You can only cancel your own reservations");
		}
		Instant now = now();
		switch (r.statusAt(now)) {
			case UPCOMING -> r.cancel(now);
			case ACTIVE -> r.endEarly(now);
			case FINISHED, CANCELLED -> throw new BadRequestException("Reservation is already over");
		}
		return r;
	}

	@Transactional(readOnly = true)
	public List<Reservation> findForUser(AppUser user) {
		return reservations.findByUser(user.getId());
	}

	private void validateRange(Instant start, Instant end, Instant now) {
		if (start == null || end == null) {
			throw new BadRequestException("Start and end are required");
		}
		if (!end.isAfter(start)) {
			throw new BadRequestException("End must be after start");
		}
		if (start.isBefore(now.minus(START_GRACE))) {
			throw new BadRequestException("Start cannot be in the past");
		}
		if (Duration.between(start, end).compareTo(MAX_DURATION) > 0) {
			throw new BadRequestException("A reservation can last at most " + MAX_DURATION.toDays() + " days");
		}
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}
}
