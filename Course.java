package com.fees;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity
public class Course {
  @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
  @Column(unique=true) public String name;
  public BigDecimal fee;
}
