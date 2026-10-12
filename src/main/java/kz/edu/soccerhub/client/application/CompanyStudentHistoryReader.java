package kz.edu.soccerhub.client.application;
import kz.edu.soccerhub.common.dto.company.*;
import kz.edu.soccerhub.common.port.CompanyStudentHistoryPort;
import kz.edu.soccerhub.common.exception.NotFoundException;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.*;
import java.time.*;
@Service @RequiredArgsConstructor
public class CompanyStudentHistoryReader implements CompanyStudentHistoryPort {
 private final NamedParameterJdbcTemplate jdbc;
 public CompanyEntry student(UUID id,Set<UUID> branches) {
  if(branches.isEmpty())throw new NotFoundException("Ученик не найден",id);
  var rows=jdbc.query("select p.id,cl.branch_id,p.first_name,p.last_name,p.birth_date,cl.first_name contact_first,cl.last_name contact_last,cl.phone,cl.email,cl.status from players p join client_profiles cl on cl.id=p.parent_id where p.id=:id and cl.branch_id in (:ids)",new MapSqlParameterSource("id",id).addValue("ids",branches),(r,n)->CompanyEntry.builder().id(id).branchId(r.getObject("branch_id",UUID.class)).name(r.getString("first_name")+" "+r.getString("last_name")).contact(r.getString("contact_first")+" "+r.getString("contact_last")).phone(r.getString("phone")).email(r.getString("email")).status(r.getString("status")).birthDate(r.getObject("birth_date",LocalDate.class)).build());
  if(rows.isEmpty())throw new NotFoundException("Ученик не найден",id);return rows.getFirst();
 }
}
