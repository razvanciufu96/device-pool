package com.devicepool.device;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devicepool.common.BadRequestException;
import com.devicepool.reservation.BookingSlot;
import com.devicepool.reservation.ReservationQueries;

/**
 * The device pool with availability for a time window.
 *
 * Without parameters the window is "right now", which answers "what's free now?".
 * The frontend passes tomorrow's working hours to answer "what's free tomorrow?".
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

	private final DeviceRepository devices;
	private final ReservationQueries reservationQueries;
	private final Clock clock;

	public DeviceController(DeviceRepository devices, ReservationQueries reservationQueries, Clock clock) {
		this.devices = devices;
		this.reservationQueries = reservationQueries;
		this.clock = clock;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<DeviceDto> list(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		Instant windowFrom = from != null ? from : clock.instant();
		// A zero-length window would match nothing, so "now" is the first second from now.
		Instant windowTo = to != null ? to : windowFrom.plusSeconds(1);
		if (!windowTo.isAfter(windowFrom)) {
			throw new BadRequestException("'to' must be after 'from'");
		}

		Map<Long, List<BookingSlot>> bookingsByDevice = reservationQueries.bookingsByDevice(windowFrom, windowTo);

		return devices.findAll().stream()
				.sorted(Comparator.comparing(Device::getType).thenComparing(Device::getName, String.CASE_INSENSITIVE_ORDER))
				.map(d -> DeviceDto.from(d, bookingsByDevice.getOrDefault(d.getId(), List.of())))
				.toList();
	}

	/**
	 * {@code bookings} lists the reservations overlapping the requested window.
	 * {@code available} means: not damaged and no bookings in the window.
	 */
	public record DeviceDto(Long id, String name, DeviceType type, String os, String assetTag,
			boolean damaged, boolean available, List<BookingSlot> bookings) {

		static DeviceDto from(Device d, List<BookingSlot> bookings) {
			return new DeviceDto(d.getId(), d.getName(), d.getType(), d.getOs(), d.getAssetTag(),
					d.isDamaged(), !d.isDamaged() && bookings.isEmpty(), bookings);
		}
	}
}
