package spl.reborn.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.problem.entity.Analysis;


import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Analysis findTopByProblem_ProblemIdOrderByTurnDesc(Long problemId);

    List<Analysis> findByProblem_ProblemIdOrderByTurnAsc(Long problemId);
    Optional<Analysis> findFirstByProblem_ProblemIdOrderByTurnAsc(Long problemId);
}