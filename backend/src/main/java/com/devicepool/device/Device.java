package com.devicepool.device;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Device {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@Enumerated(EnumType.STRING)
	private DeviceType type;

	private String os;

	private String assetTag;

	protected Device() {
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public DeviceType getType() {
		return type;
	}

	public String getOs() {
		return os;
	}

	public String getAssetTag() {
		return assetTag;
	}
}
