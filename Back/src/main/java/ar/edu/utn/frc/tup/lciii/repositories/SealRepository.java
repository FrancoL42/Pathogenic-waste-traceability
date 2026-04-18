package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.SealEntity;
import ar.edu.utn.frc.tup.lciii.models.SealStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@Repository
public interface SealRepository extends JpaRepository<SealEntity,Long> {

    long countByState(SealStatus state);

    @Query("SELECT s FROM SealEntity s WHERE s.state = :state ORDER BY s.sealNumber ASC")
    List<SealEntity> findByStateOrderBySealNumberAsc(
            @Param("state") SealStatus state,
            Pageable pageable
    );
    List<SealEntity> findByGeneratorEntityAndStateIn(GeneratorEntity generatorEntity, Collection<SealStatus> state);

    List<SealEntity> findByStateAndFechaTratamiento(SealStatus state, LocalDate fechaTratamiento);

    List<SealEntity> findByStateAndFechaTratamientoAndGeneratorEntity(
            SealStatus state, LocalDate fechaTratamiento, GeneratorEntity generatorEntity);

    // Otros métodos que puedas tener
    List<SealEntity> findByState(SealStatus state);
    List<SealEntity> findByGeneratorEntityAndState(GeneratorEntity generatorEntity, SealStatus state);

    List<SealEntity> findByStateAndFechaTratamientoAndGeneratorEntity_GeneratorId(
            SealStatus state, LocalDate fechaTratamiento, Long generatorId);

    int countByGeneratorEntityAndState(GeneratorEntity generatorEntity, SealStatus state);
}

