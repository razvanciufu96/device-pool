package com.devicepool.damage;

import java.time.Clock;
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
import com.devicepool.reservation.BookingSlot;
import com.devicepool.reservation.ReservationQueries;
import com.devicepool.user.AppUser;
import com.devicepool.user.Role;

@Service
public class DamageService {

	/** Damage isn't always noticed by the next person, so show a few previous holders. */
	static final int HOLDERS_SHOWN = 3;

	private final DamageReportRepository reports;
	private final DeviceRepository devices;
	private final ReservationQueries reservationQueries;
	private final Clock clock;

	public DamageService(DamageReportRepository reports, DeviceRepository devices,
			ReservationQueries reservationQueries, Clock clock) {
		this.reports = reports;
		this.devices = devices;
		this.reservationQueries = reservationQueries;
		this.clock = clock;
	}

	/** Anyone can report damage. The device is taken out of the bookable pool until resolved. */
	@Transactional
	public DamageReport report(AppUser reporter, Long deviceId, String description) {
		if (description == null || description.isBlank()) {
			throw new BadRequestException("Please describe the damage");
		}
		// Same lock as booking, so a booking can't slip in while the device is being marked damaged.
		Device device = devices.findByIdForUpdate(deviceId)
				.orElseThrow(() -> new NotFoundException("Device " + deviceId + " not found"));
		device.setDamaged(true);
		return reports.save(new DamageReport(device, reporter, description.strip(), now()));
	}

	/** Only the facility manager decides a device is fit for use again. */
	@Transactional
	public DamageReport resolve(AppUser user, Long reportId) {
		if (user.getRole() != Role.FACILITY) {
			throw new ForbiddenException("Only facility management can resolve damage reports");
		}
		DamageReport report = reports.findWithDetailsById(reportId)
				.orElseThrow(() -> new NotFoundException("Damage report " + reportId + " not found"));
		if (!report.isOpen()) {
			throw new BadRequestException("Damage report is already resolved");
		}
		Device device = devices.findByIdForUpdate(report.getDevice().getId()).orElseThrow();
		report.resolve(user, now());
		reports.flush();
		device.setDamaged(reports.existsByDeviceIdAndResolvedAtIsNull(device.getId()));
		return report;
	}

	@Transactional(readOnly = true)
	public List<DamageReport> findAll() {
		return reports.findAllWithDetails();
	}

	/** Who had the device most recently before the damage was reported. */
	@Transactional(readOnly = true)
	public List<BookingSlot> lastHolders(DamageReport report) {
		return reservationQueries.recentHolders(report.getDevice().getId(), report.getReportedAt(), HOLDERS_SHOWN);
	}

	private Instant now() {
		return clock.instant().truncatedTo(ChronoUnit.SECONDS);
	}
}
