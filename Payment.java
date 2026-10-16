package com.fees;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
@Entity
public class Payment {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  public Long studentId;
  public BigDecimal amount;
  public String method;
  public LocalDate paidOn = LocalDate.now();
}
