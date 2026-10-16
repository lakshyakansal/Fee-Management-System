package com.fees;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api")
public class FeeController {
  private final StudentRepo students; private final PaymentRepo payments; private final LogRepo logs;
  public FeeController(StudentRepo s, PaymentRepo p, LogRepo l){ students=s; payments=p; logs=l; }

  @GetMapping("/students")
  public List<Map<String,Object>> list(){
    List<Map<String,Object>> out = new ArrayList<>();
    for (Student s : students.findAll()) {
      BigDecimal paid = payments.paidBy(s.id), due = s.totalFee.subtract(paid);
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("id",s.id); m.put("name",s.name); m.put("email",s.email); m.put("course",s.course);
      m.put("totalFee",s.totalFee); m.put("paid",paid); m.put("due",due);
      m.put("status", due.signum()<=0 ? "PAID" : paid.signum()>0 ? "PARTIAL" : "UNPAID");
      out.add(m);
    }
    return out;
  }

  @PostMapping("/students")
  public Student add(@RequestBody Student s){
    if (s.name==null || s.name.isBlank() || s.totalFee==null || s.totalFee.signum()<0)
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name and valid fee required");
    s.id=null; Student saved = students.save(s);
    logs.save(new ActivityLog("STUDENT_ADDED", saved.name+" ("+saved.course+") fee "+saved.totalFee));
    return saved;
  }

  @DeleteMapping("/students/{id}") @Transactional
  public void delete(@PathVariable Long id){
    Student s = students.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    payments.deleteByStudentId(id); students.delete(s);
    logs.save(new ActivityLog("STUDENT_DELETED", s.name));
  }

  @PostMapping("/payments")
  public Payment pay(@RequestBody Payment p){
    Student s = students.findById(p.studentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,"Student not found"));
    if (p.amount==null || p.amount.signum()<=0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid amount");
    BigDecimal due = s.totalFee.subtract(payments.paidBy(s.id));
    if (p.amount.compareTo(due)>0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Amount exceeds due "+due);
    p.id=null; if (p.paidOn==null) p.paidOn=java.time.LocalDate.now();
    Payment saved = payments.save(p);
    logs.save(new ActivityLog("PAYMENT", s.name+" paid "+p.amount+" via "+p.method));
    return saved;
  }

  @GetMapping("/students/{id}/payments")
  public List<Payment> history(@PathVariable Long id){ return payments.findByStudentIdOrderByPaidOnDesc(id); }

  @GetMapping("/stats")
  public Map<String,Object> stats(){
    BigDecimal total=BigDecimal.ZERO, paid=BigDecimal.ZERO; List<Student> all = students.findAll();
    for (Student s: all){ total=total.add(s.totalFee); paid=paid.add(payments.paidBy(s.id)); }
    return Map.of("students",all.size(),"total",total,"collected",paid,"pending",total.subtract(paid));
  }

  @GetMapping("/logs") public List<ActivityLog> logs(){ return logs.findTop20ByOrderByTimeDesc(); }
}
