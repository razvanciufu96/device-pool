package com.devicepool.damage;

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

@Entity
public class DamageReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "device_id")
	private Device device;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reported_by")
	private AppUser reportedBy;

	private String description;

	private Instant reportedAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "resolved_by")
	private AppUser resolvedBy;

	private Instant resolvedAt;

	protected DamageReport() {
	}

	DamageReport(Device device, AppUser reportedBy, String description, Instant reportedAt) {
		this.device = device;
		this.reportedBy = reportedBy;
		this.description = description;
		this.reportedAt = reportedAt;
	}

	void resolve(AppUser by, Instant at) {
		this.resolvedBy = by;
		this.resolvedAt = at;
	}

	public boolean isOpen() {
		return resolvedAt == null;
	}

	public Long getId() {
		return id;
	}

	public Device getDevice() {
		return device;
	}

	public AppUser getReportedBy() {
		return reportedBy;
	}

	public String getDescription() {
		return description;
	}

	public Instant getReportedAt() {
		return reportedAt;
	}

	public AppUser getResolvedBy() {
		return resolvedBy;
	}

	public Instant getResolvedAt() {
		return resolvedAt;
	}
}
