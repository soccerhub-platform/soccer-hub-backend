package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
@Builder
public record CompanyQuery(Set<UUID> branchIds, String search, String status, LocalDate dateFrom,
 LocalDate dateTo, int page, int size, boolean filterDates, String attention, String sort, UUID ownerId) {
 public CompanyQuery(Set<UUID> branchIds,String search,String status,LocalDate dateFrom,LocalDate dateTo,int page,int size,boolean filterDates,String attention) {this(branchIds,search,status,dateFrom,dateTo,page,size,filterDates,attention, "", null);}
 public CompanyQuery(Set<UUID> branchIds,String search,String status,LocalDate from,LocalDate to,int page,int size,boolean filterDates,String attention,String sort) {this(branchIds,search,status,from,to,page,size,filterDates,attention,sort,null);}
 public CompanyQuery withOwner(UUID owner) {return new CompanyQuery(branchIds,search,status,dateFrom,dateTo,page,size,filterDates,attention,sort,owner);}
 public CompanyQuery withSort(String value) {return new CompanyQuery(branchIds,search,status,dateFrom,dateTo,page,size,filterDates,attention,value,ownerId);}
}
