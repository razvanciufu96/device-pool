package com.devicepool.damage;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devicepool.reservation.BookingSlot;
import com.devicepool.user.CurrentUserService;

@RestController
public class DamageController {

	private final DamageService service;
	private final CurrentUserService currentUser;

	public DamageController(DamageService service, CurrentUserService currentUser) {
		this.service = service;
		this.currentUser = currentUser;
	}

	@PostMapping("/api/devices/{deviceId}/damage-reports")
	@ResponseStatus(HttpStatus.CREATED)
	public DamageReportDto report(@RequestHeader(value = CurrentUserService.HEADER, required = false) Long userId,
			@PathVariable Long deviceId, @RequestBody ReportDamageRequest request) {
		DamageReport report = service.report(currentUser.require(userId), deviceId, request.description());
		return toDto(report);
	}

	@GetMapping("/api/damage-reports")
	public List<DamageReportDto> list() {
		return service.findAll().stream().map(this::toDto).toList();
	}

	@PostMapping("/api/damage-reports/{id}/resolve")
	public DamageReportDto resolve(@RequestHeader(value = CurrentUserService.HEADER, required = false) Long userId,
			@PathVariable Long id) {
		return toDto(service.resolve(currentUser.require(userId), id));
	}

	private DamageReportDto toDto(DamageReport r) {
		return new DamageReportDto(r.getId(), r.getDevice().getId(), r.getDevice().getName(),
				r.getDevice().getAssetTag(), r.getDescription(), r.getReportedBy().getName(), r.getReportedAt(),
				r.isOpen(), r.getResolvedBy() != null ? r.getResolvedBy().getName() : null, r.getResolvedAt(),
				service.lastHolders(r));
	}

	public record ReportDamageRequest(String description) {
	}

	public record DamageReportDto(Long id, Long deviceId, String deviceName, String assetTag, String description,
			String reportedBy, Instant reportedAt, boolean open, String resolvedBy, Instant resolvedAt,
			List<BookingSlot> lastHolders) {
	}
}
