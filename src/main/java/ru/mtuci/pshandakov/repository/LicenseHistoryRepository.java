package ru.mtuci.pshandakov.repository;

import ru.mtuci.pshandakov.model.LicenseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, Long> {

}
