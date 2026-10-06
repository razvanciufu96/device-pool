package com.devicepool.device;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface DeviceRepository extends JpaRepository<Device, Long> {

	Optional<Device> findByAssetTag(String assetTag);

	/**
	 * SELECT ... FOR UPDATE on the device row. Used to serialize bookings per device:
	 * two concurrent bookings for the same device queue up here, so the second one
	 * sees the first one's reservation when it checks for overlaps.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select d from Device d where d.id = :id")
	Optional<Device> findByIdForUpdate(@Param("id") Long id);
}
