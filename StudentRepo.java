package com.fees;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface StudentRepo extends JpaRepository<Student,Long> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)            // SELECT ... FOR UPDATE
  @Query("select s from Student s where s.id=?1")
  Optional<Student> lockById(Long id);
}
