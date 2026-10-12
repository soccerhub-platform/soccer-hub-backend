package kz.edu.soccerhub.organization.application.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.edu.soccerhub.common.dto.branch.*;
import kz.edu.soccerhub.common.port.BranchHistoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.domain.AuditorAware;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@Service
@RequiredArgsConstructor
public class BranchHistoryService implements BranchHistoryPort {
 private final NamedParameterJdbcTemplate jdbc;
 private final ObjectMapper mapper;
 private final AuditorAware<String> auditor;
 public void record(String event,BranchDto before,BranchDto after) {
  try {
   jdbc.update("insert into branch_history(id,branch_id,event,changed_by,before_snapshot,after_snapshot) values (:id,:branch,:event,:actor,cast(:before as jsonb),cast(:after as jsonb))",
    new MapSqlParameterSource("id",UUID.randomUUID()).addValue("branch",after.id()).addValue("event",event)
     .addValue("actor",auditor.getCurrentAuditor().orElse("system")).addValue("before",before==null?null:mapper.writeValueAsString(before)).addValue("after",mapper.writeValueAsString(after)));
  } catch(com.fasterxml.jackson.core.JsonProcessingException e) { throw new IllegalStateException("Не удалось сохранить историю филиала",e); }
 }
 @Transactional(readOnly=true)
 public Page<BranchHistoryOutput> history(UUID branch,int page,int size) {
  if(page<0 || page>100000 || size<1 || size>100) throw new kz.edu.soccerhub.common.exception.BadRequestException("Некорректная страница истории");
  var params=new MapSqlParameterSource("branch",branch).addValue("limit",size).addValue("offset",(long)page*size);
  long count=Objects.requireNonNull(jdbc.queryForObject("select count(*) from branch_history where branch_id=:branch",params,Long.class));
  var rows=jdbc.query("select * from branch_history where branch_id=:branch order by created_at desc,id desc limit :limit offset :offset",params,(rs,n)-> {
   try {return new BranchHistoryOutput(rs.getObject("id",UUID.class),rs.getString("event"),rs.getString("changed_by"),rs.getTimestamp("created_at").toInstant(),
    rs.getString("before_snapshot")==null?null:mapper.readValue(rs.getString("before_snapshot"),BranchDto.class),mapper.readValue(rs.getString("after_snapshot"),BranchDto.class));}
   catch(com.fasterxml.jackson.core.JsonProcessingException e) {throw new IllegalStateException("Не удалось прочитать историю",e);}
  });
  return new PageImpl<>(rows,PageRequest.of(page,size),count);
 }
}
