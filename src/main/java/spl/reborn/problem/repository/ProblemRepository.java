package spl.reborn.problem.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.problem.entity.Problem;

public interface ProblemRepository extends JpaRepository<Problem, Long> {}
