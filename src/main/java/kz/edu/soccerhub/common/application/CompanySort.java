package kz.edu.soccerhub.common.application;
import kz.edu.soccerhub.common.exception.BadRequestException;
import java.util.Map;
public final class CompanySort {
 private CompanySort() {}
 public static String order(String sort,String fallback,Map<String,String> allowed) {
  if(sort==null || sort.isBlank()) return fallback;
  String sql=allowed.get(sort);
  if(sql==null) throw new BadRequestException("Недопустимая сортировка");
  return sql;
 }
}
