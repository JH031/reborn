// spl.reborn.study.repository.UserStudyRepository
package spl.reborn.study.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import spl.reborn.study.entity.UserStudy;

public interface UserStudyRepository extends JpaRepository<UserStudy, Long> {}
