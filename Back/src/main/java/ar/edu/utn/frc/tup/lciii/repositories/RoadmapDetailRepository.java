package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.entities.GeneratorEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapDetailEntity;
import ar.edu.utn.frc.tup.lciii.entities.RoadmapEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoadmapDetailRepository extends JpaRepository<RoadmapDetailEntity, Long> {
    List<RoadmapDetailEntity> findByRoadmapEntity(RoadmapEntity roadmapEntity);
    List<RoadmapDetailEntity> findByGeneratorEntity(GeneratorEntity generatorEntity);
}
