package com.fees;
import org.springframework.data.jpa.repository.*;
import java.math.BigDecimal;
import java.util.List;
public interface PaymentRepo extends JpaRepository<Payment,Long> {
  @Query("select coalesce(sum(p.amount),0) from Payment p where p.studentId=?1") BigDecimal paidBy(Long studentId);
  List<Payment> findByStudentIdOrderByPaidOnDesc(Long studentId);
  void deleteByStudentId(Long studentId);
}
