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

    // ✅ problemId를 기준으로 모든 Analysis를 turn 번호 오름차순으로 조회
    List<Analysis> findByProblem_ProblemIdOrderByTurnAsc(Long problemId);

}