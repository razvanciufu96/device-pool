package com.devicepool.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.devicepool.common.BadRequestException;
import com.devicepool.common.ForbiddenException;
import com.devicepool.device.DeviceRepository;
import com.devicepool.user.AppUser;
import com.devicepool.user.AppUserRepository;

/** Runs against the real (in-memory H2) database so locking and queries are exercised for real. */
@SpringBootTest
class ReservationServiceTest {

	@Autowired
	ReservationService service;

	@Autowired
	ReservationRepository reservations;

	@Autowired
	DeviceRepository devices;

	@Autowired
	AppUserRepository users;

	// Far enough in the future that "start in the past" validation never interferes.
	final Instant base = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

	@AfterEach
	void cleanUp() {
		reservations.deleteAll();
	}

	@Test
	void rejectsOverlappingReservation() {
		service.create(ana(), pixel(), at(10), at(12));

		assertThatThrownBy(() -> service.create(mihai(), pixel(), at(11), at(13)))
				.isInstanceOf(ReservationConflictException.class)
				.satisfies(e -> assertThat(((ReservationConflictException) e).getConflicts())
						.singleElement()
						.extracting(BookingSlot::userName).isEqualTo("Ana Popescu"));
	}

	@Test
	void rejectsReservationFullyInsideAnother() {
		service.create(ana(), pixel(), at(9), at(17));

		assertThatThrownBy(() -> service.create(mihai(), pixel(), at(12), at(13)))
				.isInstanceOf(ReservationConflictException.class);
	}

	@Test
	void allowsBackToBackReservations() {
		service.create(ana(), pixel(), at(10), at(12));

		Reservation next = service.create(mihai(), pixel(), at(12), at(14));

		assertThat(next.getId()).isNotNull();
	}

	@Test
	void sameTimeOnDifferentDeviceIsFine() {
		service.create(ana(), pixel(), at(10), at(12));

		Reservation other = service.create(mihai(), iphone(), at(10), at(12));

		assertThat(other.getId()).isNotNull();
	}

	@Test
	void cancelledReservationFreesTheSlot() {
		Reservation r = service.create(ana(), pixel(), at(10), at(12));
		service.cancel(ana(), r.getId());

		Reservation again = service.create(mihai(), pixel(), at(10), at(12));

		assertThat(again.getId()).isNotNull();
	}

	@Test
	void cannotCancelSomeoneElsesReservation() {
		Reservation r = service.create(ana(), pixel(), at(10), at(12));

		assertThatThrownBy(() -> service.cancel(mihai(), r.getId()))
				.isInstanceOf(ForbiddenException.class);
	}

	@Test
	void rejectsInvalidRanges() {
		assertThatThrownBy(() -> service.create(ana(), pixel(), at(12), at(10)))
				.isInstanceOf(BadRequestException.class);
		assertThatThrownBy(() -> service.create(ana(), pixel(), at(12), at(12)))
				.isInstanceOf(BadRequestException.class);
		Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
		assertThatThrownBy(() -> service.create(ana(), pixel(), yesterday, yesterday.plusSeconds(3600)))
				.isInstanceOf(BadRequestException.class);
	}

	/**
	 * The core guarantee: many people booking the same slot at the same moment.
	 * Without the row lock, several threads could pass the overlap check before any of them
	 * inserts. With it, exactly one booking wins.
	 */
	@Test
	void concurrentBookingsOfSameSlotProduceExactlyOneReservation() throws Exception {
		int attempts = 10;
		ExecutorService pool = Executors.newFixedThreadPool(attempts);
		CountDownLatch startTogether = new CountDownLatch(1);
		AppUser user = ana();
		Long deviceId = pixel();

		List<Future<Boolean>> results = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			Callable<Boolean> attempt = () -> {
				startTogether.await();
				try {
					service.create(user, deviceId, at(10), at(12));
					return true;
				}
				catch (ReservationConflictException e) {
					return false;
				}
			};
			results.add(pool.submit(attempt));
		}
		startTogether.countDown();

		int successes = 0;
		for (Future<Boolean> result : results) {
			if (result.get()) {
				successes++;
			}
		}
		pool.shutdown();

		assertThat(successes).isEqualTo(1);
		assertThat(reservations.findOverlapping(deviceId, at(0), at(23))).hasSize(1);
	}

	private Instant at(int hour) {
		return base.plus(hour, ChronoUnit.HOURS);
	}

	private AppUser ana() {
		return users.findByEmail("ana@example.com").orElseThrow();
	}

	private AppUser mihai() {
		return users.findByEmail("mihai@example.com").orElseThrow();
	}

	private Long pixel() {
		return devices.findByAssetTag("DP-003").orElseThrow().getId();
	}

	private Long iphone() {
		return devices.findByAssetTag("DP-001").orElseThrow().getId();
	}
}
