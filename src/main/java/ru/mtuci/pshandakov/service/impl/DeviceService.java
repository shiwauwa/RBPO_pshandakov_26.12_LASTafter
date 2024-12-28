package ru.mtuci.pshandakov.service.impl;

import lombok.RequiredArgsConstructor;
import ru.mtuci.pshandakov.model.Device;
import ru.mtuci.pshandakov.model.DeviceLicense;
import ru.mtuci.pshandakov.model.License;
import ru.mtuci.pshandakov.repository.DeviceLicenseRepository;
import ru.mtuci.pshandakov.repository.DeviceRepository;
import ru.mtuci.pshandakov.repository.LicenseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceLicenseRepository deviceLicenseRepository;
    private final LicenseRepository licenseRepository;

    public Device saveDevice(Device device) {
        return deviceRepository.save(device);
    }


    public Optional<Device> getDeviceById(Long id) {
        return deviceRepository.findById(id);
    }


    public Device getDeviceByMacAddress(String macAddress) {
        return deviceRepository.findByMacAddress(macAddress);
    }

    public Optional<Device> getDeviceByMacAddressAndName(String macAddress, String deviceName) {
        return deviceRepository.findByMacAddressAndName(macAddress, deviceName);
    }

    public List<Device> getAllDevices() {
        return deviceRepository.findAll();
    }


    public void deleteDevice(Long id) {
        Optional<Device> deviceOpt = deviceRepository.findById(id);

        if (deviceOpt.isPresent()) {
            Device device = deviceOpt.get();
            Optional<DeviceLicense> deviceLicenseOpt = deviceLicenseRepository.findByDeviceId(id);
            if (deviceLicenseOpt.isPresent()) {
                DeviceLicense deviceLicense = deviceLicenseOpt.get();
                Optional<License> licenseOpt = licenseRepository.findById(deviceLicense.getLicense().getId());
                License license = licenseOpt.get();
                license.setDeviceCount(license.getDeviceCount() + 1);
                licenseRepository.save(license);
                deviceLicenseRepository.delete(deviceLicense);
            }

            deviceRepository.delete(device);
        }
    }

    public boolean isMacAddressExists(String macAddress) {
        return deviceRepository.existsByMacAddress(macAddress);
    }

    public boolean isDeviceExists(String macAddress, String name) {
        Optional<Device> device = deviceRepository.findByMacAddressAndName(macAddress, name);
        return device.isPresent();
    }
}
