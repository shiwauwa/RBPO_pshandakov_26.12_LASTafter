package ru.mtuci.pshandakov.repository;

import ru.mtuci.pshandakov.model.DeviceLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {


    Optional<DeviceLicense> findByLicenseId(Long licenseId);

    Optional<DeviceLicense> findByDeviceId(Long deviceId);

    Optional<DeviceLicense> findByDeviceIdAndLicenseId(Long deviceId, Long licenseId);

}
