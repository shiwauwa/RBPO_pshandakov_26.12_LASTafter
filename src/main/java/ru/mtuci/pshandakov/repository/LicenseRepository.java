package ru.mtuci.pshandakov.repository;

import ru.mtuci.pshandakov.model.License;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByCode(String code);


}

