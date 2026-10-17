package com.fees;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
public class Student {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String name, phone, email, course;
  public Long courseId;
  public BigDecimal totalFee;                    // copied from the course at enrolment
  public BigDecimal discount = BigDecimal.ZERO;  // discount / scholarship
}
