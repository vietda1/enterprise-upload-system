package com.msb.upload.repository;

import com.msb.upload.model.DatasetConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface DatasetConfigRepository extends JpaRepository<DatasetConfig, Integer> {
    Optional<DatasetConfig> findByCode(String code);
    Optional<DatasetConfig> findByCodeAndIsActiveTrue(String code);
    List<DatasetConfig> findByIsActiveTrue();
}
