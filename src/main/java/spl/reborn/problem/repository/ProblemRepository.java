package spl.reborn.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.problem.entity.Problem;

import java.util.List;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    List<Problem> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
