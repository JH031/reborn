package spl.reborn.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import spl.reborn.problem.entity.Analysis;
import spl.reborn.problem.entity.Problem;

import java.util.List;
import java.util.Optional;

public interface AnalysisRepository extends JpaRepository<Analysis, Long> {

    Analysis findTopByProblem_ProblemIdOrderByTurnDesc(Long problemId);

    List<Analysis> findTop2ByProblem_ProblemIdOrderByTurnDesc(Long problemId); // 최신 2개 컨텍스트용
}