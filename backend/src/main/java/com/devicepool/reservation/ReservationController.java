package com.devicepool.reservation;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devicepool.user.AppUser;
import com.devicepool.user.CurrentUserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

	private final ReservationService service;
	private final CurrentUserService currentUser;
	private final Clock clock;

	public ReservationController(ReservationService service, CurrentUserService currentUser, Clock clock) {
		this.service = service;
		this.currentUser = currentUser;
		this.clock = clock;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReservationDto create(@RequestHeader(value = CurrentUserService.HEADER, required = false) Long userId,
			@Valid @RequestBody CreateReservationRequest request) {
		AppUser user = currentUser.require(userId);
		Reservation r = service.create(user, request.deviceId(), request.start(), request.end());
		return ReservationDto.from(r, clock.instant());
	}

	@GetMapping("/mine")
	public List<ReservationDto> mine(@RequestHeader(value = CurrentUserService.HEADER, required = false) Long userId) {
		AppUser user = currentUser.require(userId);
		Instant now = clock.instant();
		return service.findForUser(user).stream().map(r -> ReservationDto.from(r, now)).toList();
	}

	@DeleteMapping("/{id}")
	public ReservationDto cancel(@RequestHeader(value = CurrentUserService.HEADER, required = false) Long userId,
			@PathVariable Long id) {
		AppUser user = currentUser.require(userId);
		return ReservationDto.from(service.cancel(user, id), clock.instant());
	}

	public record CreateReservationRequest(@NotNull Long deviceId, @NotNull Instant start, @NotNull Instant end) {
	}

	public record ReservationDto(Long id, Long deviceId, String deviceName, Instant start, Instant end,
			ReservationStatus status) {

		static ReservationDto from(Reservation r, Instant now) {
			return new ReservationDto(r.getId(), r.getDevice().getId(), r.getDevice().getName(),
					r.getStartAt(), r.getEndAt(), r.statusAt(now));
		}
	}
}
