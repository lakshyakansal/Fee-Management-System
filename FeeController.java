package com.fees;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.*;

@RestController @RequestMapping("/api")
public class FeeController {
  private final StudentRepo students; private final PaymentRepo payments; private final LogRepo logs; private final CourseRepo courses;
  public FeeController(StudentRepo s, PaymentRepo p, LogRepo l, CourseRepo c){ students=s; payments=p; logs=l; courses=c; }

  private static ResponseStatusException bad(String m){ return new ResponseStatusException(HttpStatus.BAD_REQUEST,m); }
  private static ResponseStatusException nf(String m){ return new ResponseStatusException(HttpStatus.NOT_FOUND,m); }
  private static BigDecimal payable(Student s){ return s.totalFee.subtract(s.discount==null?BigDecimal.ZERO:s.discount); }
  private static String q(String v){ return "\""+(v==null?"":v.replace("\"","\"\""))+"\""; }

  // ---------- Courses ----------
  @GetMapping("/courses") public List<Course> courses(){ return courses.findAll(); }

  @PostMapping("/courses")
  public Course addCourse(@RequestBody Course c){
    if (c.name==null || c.name.isBlank() || c.fee==null || c.fee.signum()<=0) throw bad("Course name and fee above 0 required");
    c.name = c.name.trim();
    Course x = courses.findByName(c.name).orElseGet(() -> { c.id=null; return c; });  // add, or update fee if name exists
    x.fee = c.fee;
    Course saved = courses.save(x);
    logs.save(new ActivityLog("COURSE_SAVED", saved.name+" fee "+saved.fee));
    return saved;
  }

  // ---------- Students ----------
  @GetMapping("/students")
  public List<Map<String,Object>> list(){
    List<Map<String,Object>> out = new ArrayList<>();
    for (Student s : students.findAll()) {
      BigDecimal pay = payable(s), paid = payments.paidBy(s.id), due = pay.subtract(paid);
      Map<String,Object> m = new LinkedHashMap<>();
      m.put("id",s.id); m.put("studentNo",String.format("S%04d",s.id)); m.put("name",s.name); m.put("phone",s.phone);
      m.put("course",s.course); m.put("totalFee",s.totalFee); m.put("discount",s.discount);
      m.put("payable",pay); m.put("paid",paid); m.put("due",due);
      m.put("status", due.signum()<=0 ? "PAID" : paid.signum()>0 ? "PARTIAL" : "UNPAID");
      out.add(m);
    }
    return out;
  }

  @PostMapping("/students")
  public Student add(@RequestBody Student s){
    if (s.name==null || s.name.isBlank()) throw bad("Name required");
    if (s.courseId!=null){
      Course c = courses.findById(s.courseId).orElseThrow(() -> bad("Course not found"));
      s.course = c.name; s.totalFee = c.fee;                 // fee copied, later course fee changes won't affect this student
    }
    if (s.totalFee==null || s.totalFee.signum()<=0) throw bad("Select a course or enter a fee above 0");
    if (s.discount==null) s.discount = BigDecimal.ZERO;
    if (s.discount.signum()<0 || s.discount.compareTo(s.totalFee)>0) throw bad("Discount must be between 0 and the fee");
    s.id = null; Student saved = students.save(s);
    logs.save(new ActivityLog("STUDENT_ADDED", saved.name+" ("+saved.course+") payable "+payable(saved)));
    return saved;
  }

  @DeleteMapping("/students/{id}") @Transactional
  public void delete(@PathVariable Long id){
    Student s = students.findById(id).orElseThrow(() -> nf("Student not found"));
    payments.deleteByStudentId(id); students.delete(s);
    logs.save(new ActivityLog("STUDENT_DELETED", s.name));
  }

  // ---------- Payments ----------
  @PostMapping("/payments") @Transactional
  public Payment pay(@RequestBody Payment p){
    if (p.studentId==null) throw bad("Select a student");
    Student s = students.lockById(p.studentId).orElseThrow(() -> nf("Student not found")); // row locked until commit
    if (p.amount==null || p.amount.signum()<=0) throw bad("Invalid amount");
    BigDecimal due = payable(s).subtract(payments.paidBy(s.id));
    if (p.amount.compareTo(due)>0) throw bad("Amount exceeds due "+due);
    p.id=null; if (p.paidOn==null) p.paidOn=java.time.LocalDate.now();
    Payment saved = payments.save(p);
    logs.save(new ActivityLog("PAYMENT", String.format("R%05d ", saved.id)+s.name+" paid "+p.amount+" via "+p.method));
    return saved;
  }

  @GetMapping("/payments")
  public List<Map<String,Object>> recent(){
    Map<Long,Student> m = new HashMap<>(); students.findAll().forEach(s -> m.put(s.id,s));
    List<Payment> all = new ArrayList<>(payments.findAll());
    all.sort((a,b) -> Long.compare(b.id,a.id));
    List<Map<String,Object>> out = new ArrayList<>();
    for (Payment p : all.subList(0, Math.min(20, all.size()))) {
      Student s = m.get(p.studentId);
      out.add(Map.of("id",p.id,"receiptNo",String.format("R%05d",p.id),"student",s==null?"":s.name,
                     "amount",p.amount,"method",p.method==null?"":p.method,"paidOn",p.paidOn.toString()));
    }
    return out;
  }

  @DeleteMapping("/payments/{id}")                       // void: amount returns to the student's due
  public void voidPayment(@PathVariable Long id){
    Payment p = payments.findById(id).orElseThrow(() -> nf("Payment not found"));
    payments.delete(p);
    logs.save(new ActivityLog("PAYMENT_VOIDED", String.format("R%05d ", id)+"amount "+p.amount));
  }

  @GetMapping("/payments/{id}/receipt")
  public Map<String,Object> receipt(@PathVariable Long id){
    Payment p = payments.findById(id).orElseThrow(() -> nf("Payment not found"));
    Student s = students.findById(p.studentId).orElseThrow(() -> nf("Student not found"));
    BigDecimal paid = payments.paidBy(s.id);
    Map<String,Object> m = new LinkedHashMap<>();
    m.put("receiptNo",String.format("R%05d",p.id)); m.put("date",p.paidOn.toString());
    m.put("student",s.name+" ("+String.format("S%04d",s.id)+")"); m.put("course",s.course==null?"-":s.course);
    m.put("mode",p.method+(p.reference==null||p.reference.isBlank()?"":" / "+p.reference));
    m.put("amount",p.amount); m.put("totalPaid",paid); m.put("balanceDue",payable(s).subtract(paid));
    return m;
  }

  @GetMapping("/payments/export")
  public ResponseEntity<String> export(){
    Map<Long,Student> m = new HashMap<>(); students.findAll().forEach(s -> m.put(s.id,s));
    StringBuilder sb = new StringBuilder("Receipt,Date,Student,Course,Amount,Mode,Reference\n");
    for (Payment p : payments.findAll()) {
      Student s = m.get(p.studentId);
      sb.append(String.format("R%05d",p.id)).append(',').append(p.paidOn).append(',')
        .append(q(s==null?"":s.name)).append(',').append(q(s==null?"":s.course)).append(',').append(p.amount).append(',')
        .append(q(p.method)).append(',').append(q(p.reference)).append('\n');
    }
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=payments.csv")
      .contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
  }

  @GetMapping("/students/{id}/payments")
  public List<Payment> history(@PathVariable Long id){ return payments.findByStudentIdOrderByPaidOnDesc(id); }

  // ---------- Reporting ----------
  @GetMapping("/stats")
  public Map<String,Object> stats(){
    BigDecimal total=BigDecimal.ZERO, paid=BigDecimal.ZERO; List<Student> all = students.findAll();
    for (Student s : all){ total=total.add(payable(s)); paid=paid.add(payments.paidBy(s.id)); }
    return Map.of("students",all.size(),"total",total,"collected",paid,"pending",total.subtract(paid));
  }

  @GetMapping("/logs") public List<ActivityLog> logs(){ return logs.findTop20ByOrderByTimeDesc(); }
}
