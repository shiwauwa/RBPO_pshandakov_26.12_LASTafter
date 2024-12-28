package ru.mtuci.pshandakov.service.impl;

import lombok.RequiredArgsConstructor;
import ru.mtuci.pshandakov.model.LicenseType;
import ru.mtuci.pshandakov.repository.LicenseTypeRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LicenseTypeService {

    private final LicenseTypeRepository licenseTypeRepository;

    public LicenseType saveLicenseType(LicenseType licenseType) {
        return licenseTypeRepository.save(licenseType);
    }


    public Optional<LicenseType> getLicenseTypeById(Long id) {
        return licenseTypeRepository.findById(id);
    }


    public List<LicenseType> getAllLicenseTypes() {
        return licenseTypeRepository.findAll();
    }


    public void deleteLicenseType(Long id) {
        licenseTypeRepository.deleteById(id);
    }
}
