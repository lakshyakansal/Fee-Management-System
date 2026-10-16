package com.fees;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
@Document("activity_logs")
public class ActivityLog {
  @Id public String id;
  public String action, detail;
  public LocalDateTime time = LocalDateTime.now();
  public ActivityLog(){}
  public ActivityLog(String action,String detail){ this.action=action; this.detail=detail; }
}
