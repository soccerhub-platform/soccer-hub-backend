package kz.edu.soccerhub.common.dto.branch;

import lombok.Builder;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Builder
public record BranchWorkingDay(DayOfWeek day, boolean closed, LocalTime opensAt, LocalTime closesAt) {}
