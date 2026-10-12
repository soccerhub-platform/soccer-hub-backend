package kz.edu.soccerhub.common.application;
import kz.edu.soccerhub.common.dto.company.CompanyQuery;
import kz.edu.soccerhub.common.exception.BadRequestException;
import java.time.*;
import java.util.*;
public final class CompanyQueryFactory {
 private CompanyQueryFactory() {}
 public static CompanyQuery create(Set<UUID> branches,String search,String status,LocalDate from,LocalDate to,int page,int size) {
  boolean dates=from!=null||to!=null; var today=LocalDate.now(ZoneId.of("Asia/Almaty"));
  if(dates&&(from==null||to==null))throw new BadRequestException("Укажите обе даты периода");
  if(from==null)from=today.withDayOfMonth(1);if(to==null)to=today;
  if(from.isAfter(to)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>366)throw new BadRequestException("Выберите период до 367 дней");
  if(page<0||page>100000||size<1||size>100)throw new BadRequestException("Некорректная страница");
  if(search!=null&&search.length()>200)throw new BadRequestException("Поиск слишком длинный");
  return new CompanyQuery(branches,search==null?"":search.trim(),status==null?"":status,from,to,page,size,dates,"");
 }
}
