package com.devicepool.reservation;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.devicepool.device.DeviceRepository;
import com.devicepool.user.AppUserRepository;

/**
 * Users and devices come from a Flyway migration. Reservations depend on the current date,
 * so they are created here on the first start (only when there are none yet).
 */
@Component
@ConditionalOnProperty(name = "app.demo-data", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

	private final ReservationRepository reservations;
	private final DeviceRepository devices;
	private final AppUserRepository users;
	private final Clock clock;

	public DemoDataSeeder(ReservationRepository reservations, DeviceRepository devices,
			AppUserRepository users, Clock clock) {
		this.reservations = reservations;
		this.devices = devices;
		this.users = users;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) {
		if (reservations.count() > 0) {
			return;
		}
		Instant now = clock.instant().truncatedTo(ChronoUnit.MINUTES);
		LocalDate today = LocalDate.now(clock.withZone(ZoneId.systemDefault()));

		// Running right now
		book("DP-003", "ana@example.com", now.minus(1, ChronoUnit.HOURS), now.plus(2, ChronoUnit.HOURS));
		// Starts soon
		book("DP-008", "mihai@example.com", now.plus(30, ChronoUnit.MINUTES), now.plus(3, ChronoUnit.HOURS));
		// Tomorrow
		book("DP-001", "mihai@example.com", at(today.plusDays(1), 13), at(today.plusDays(1), 17));
		book("DP-007", "elena@example.com", at(today.plusDays(1), 9), at(today.plusDays(1), 12));
		// Already over, so there is some history
		book("DP-002", "ana@example.com", at(today.minusDays(1), 10), at(today.minusDays(1), 15));
	}

	private void book(String assetTag, String email, Instant start, Instant end) {
		reservations.save(new Reservation(
				devices.findByAssetTag(assetTag).orElseThrow(),
				users.findByEmail(email).orElseThrow(),
				start, end, clock.instant()));
	}

	private static Instant at(LocalDate date, int hour) {
		return date.atTime(LocalTime.of(hour, 0)).atZone(ZoneId.systemDefault()).toInstant();
	}
}
