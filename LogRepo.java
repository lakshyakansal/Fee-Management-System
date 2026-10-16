package com.fees;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
public interface LogRepo extends MongoRepository<ActivityLog,String> { List<ActivityLog> findTop20ByOrderByTimeDesc(); }
