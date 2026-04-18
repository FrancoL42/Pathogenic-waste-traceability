package ar.edu.utn.frc.tup.lciii.repositories;

import ar.edu.utn.frc.tup.lciii.models.RetirementRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RetirementRequestRepository extends JpaRepository<RetirementRequestEntity, Long> {
    List<RetirementRequestEntity> findByRoadmapId(Long roadmapId);

}
