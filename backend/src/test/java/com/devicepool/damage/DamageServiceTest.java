package com.devicepool.damage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.devicepool.common.BadRequestException;
import com.devicepool.common.ForbiddenException;
import com.devicepool.device.Device;
import com.devicepool.device.DeviceRepository;
import com.devicepool.reservation.BookingSlot;
import com.devicepool.reservation.Reservation;
import com.devicepool.reservation.ReservationRepository;
import com.devicepool.reservation.ReservationService;
import com.devicepool.user.AppUser;
import com.devicepool.user.AppUserRepository;

@SpringBootTest
class DamageServiceTest {

	@Autowired
	DamageService damage;

	@Autowired
	ReservationService reservationService;

	@Autowired
	DamageReportRepository reports;

	@Autowired
	ReservationRepository reservations;

	@Autowired
	DeviceRepository devices;

	@Autowired
	AppUserRepository users;

	final Instant tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

	@AfterEach
	void cleanUp() {
		reports.deleteAll();
		reservations.deleteAll();
		devices.saveAll(devices.findAll().stream().peek(d -> d.setDamaged(false)).toList());
	}

	@Test
	void damagedDeviceCannotBeBooked() {
		damage.report(user("mihai"), pixel().getId(), "Battery swollen");

		assertThatThrownBy(() -> reservationService.create(user("ana"), pixel().getId(), tomorrow, tomorrow.plusSeconds(3600)))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("damaged");
	}

	@Test
	void onlyFacilityCanResolve() {
		DamageReport report = damage.report(user("mihai"), pixel().getId(), "Battery swollen");

		assertThatThrownBy(() -> damage.resolve(user("elena"), report.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void resolvingMakesDeviceBookableAgain() {
		DamageReport report = damage.report(user("mihai"), pixel().getId(), "Battery swollen");

		damage.resolve(user("radu"), report.getId());

		assertThat(pixel().isDamaged()).isFalse();
		assertThat(reservationService.create(user("ana"), pixel().getId(), tomorrow, tomorrow.plusSeconds(3600)).getId())
				.isNotNull();
	}

	@Test
	void deviceStaysDamagedWhileAnotherReportIsOpen() {
		DamageReport first = damage.report(user("mihai"), pixel().getId(), "Cracked screen");
		damage.report(user("ana"), pixel().getId(), "Charging port loose");

		damage.resolve(user("radu"), first.getId());

		assertThat(pixel().isDamaged()).isTrue();
	}

	@Test
	void lastHoldersAreThePeopleWhoHadItBeforeTheReport() {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		// Past reservations can't be created through the service, so insert them directly.
		reservations.save(new Reservation(pixel(), user("elena"), now.minus(3, ChronoUnit.DAYS), now.minus(70, ChronoUnit.HOURS), now));
		reservations.save(new Reservation(pixel(), user("ana"), now.minus(1, ChronoUnit.DAYS), now.minus(20, ChronoUnit.HOURS), now));
		// A booking for the future must not count as "had it last".
		reservations.save(new Reservation(pixel(), user("radu"), now.plus(1, ChronoUnit.DAYS), now.plus(26, ChronoUnit.HOURS), now));

		DamageReport report = damage.report(user("mihai"), pixel().getId(), "Cracked screen");

		assertThat(damage.lastHolders(report)).extracting(BookingSlot::userName)
				.containsExactly("Ana Popescu", "Elena Dumitru");
	}

	private Device pixel() {
		return devices.findByAssetTag("DP-003").orElseThrow();
	}

	private AppUser user(String name) {
		return users.findByEmail(name + "@example.com").orElseThrow();
	}
}
