package com.fees;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
public class Student {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public String name, email, course;
  public BigDecimal totalFee;
}
