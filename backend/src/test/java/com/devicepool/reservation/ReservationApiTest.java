package com.devicepool.reservation;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.devicepool.device.DeviceRepository;

/** Checks the HTTP contract: status codes and the shape of error responses. */
@SpringBootTest
@AutoConfigureMockMvc
class ReservationApiTest {

	@Autowired
	MockMvc mvc;

	@Autowired
	ReservationRepository reservations;

	@Autowired
	DeviceRepository devices;

	@AfterEach
	void cleanUp() {
		reservations.deleteAll();
	}

	@Test
	void overlappingBookingReturns409WithConflictDetails() throws Exception {
		Long pixel = devices.findByAssetTag("DP-003").orElseThrow().getId();
		Instant start = Instant.now().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);
		String body = """
				{"deviceId": %d, "start": "%s", "end": "%s"}
				""".formatted(pixel, start, start.plus(2, ChronoUnit.HOURS));

		mvc.perform(post("/api/reservations").header("X-User-Id", 1)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("UPCOMING"));

		mvc.perform(post("/api/reservations").header("X-User-Id", 2)
				.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.conflicts[0].userName").value("Ana Popescu"));
	}

	@Test
	void missingUserHeaderReturns401() throws Exception {
		mvc.perform(get("/api/reservations/mine"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void deviceListShowsAvailability() throws Exception {
		mvc.perform(get("/api/devices"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(9))
				.andExpect(jsonPath("$[0].available").value(true));
	}
}
