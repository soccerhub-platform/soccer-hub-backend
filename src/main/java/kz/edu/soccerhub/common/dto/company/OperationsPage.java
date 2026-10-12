package kz.edu.soccerhub.common.dto.company;
import lombok.Builder;
import java.util.*;
@Builder
public record OperationsPage(List<OperationsRow> content,long totalElements,int totalPages,int number,int size,Map<String,Long> summary) {
 public OperationsPage(List<OperationsRow> content,long totalElements,int totalPages,int number,int size) {this(content,totalElements,totalPages,number,size,Map.of());}
}
