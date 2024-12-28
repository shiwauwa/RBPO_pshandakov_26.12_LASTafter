package ru.mtuci.pshandakov.repository;

import ru.mtuci.pshandakov.model.ActionAuthRegHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActionAuthRegHistoryRepository extends JpaRepository<ActionAuthRegHistory, Long> {

}
