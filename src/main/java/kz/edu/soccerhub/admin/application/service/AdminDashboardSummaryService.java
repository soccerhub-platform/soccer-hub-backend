package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.dashboard.*;
import kz.edu.soccerhub.common.dto.analytics.DashboardLeadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.analytics.DashboardLeadWeeklyTrendItemOutput;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.AnalyticsPort;
import kz.edu.soccerhub.common.port.BranchPort;
import kz.edu.soccerhub.common.port.CoachPort;
import kz.edu.soccerhub.common.port.GroupCoachPort;
import kz.edu.soccerhub.common.port.GroupPort;
import kz.edu.soccerhub.common.port.GroupSchedulePort;
import kz.edu.soccerhub.common.port.PaymentPort;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardSummaryService {

    private static final String DEFAULT_TIMEZONE = "Asia/Almaty";
    private static final int OVERLOADED_COACH_WEEKLY_LIMIT = 12;

    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final BranchPort branchPort;
    private final GroupPort groupPort;
    private final GroupCoachPort groupCoachPort;
    private final GroupSchedulePort groupSchedulePort;
    private final CoachPort coachPort;
    private final PaymentPort paymentPort;
    private final AnalyticsPort analyticsPort;

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary(
            UUID adminId,
            UUID branchId,
            LocalDate date,
            String timezone,
            boolean superAdmin
    ) {
        verifyAdminAccessToBranch(adminId, branchId, superAdmin);

        ZoneId zoneId = resolveZone(timezone);
        LocalDate resolvedDate = date == null ? LocalDate.now(zoneId) : date;
        BranchDto branch = branchPort.findById(branchId).orElseThrow(() -> new NotFoundException("Branch not found", branchId));

        List<GroupDto> branchGroups = new ArrayList<>(groupPort.getGroupsByBranch(branchId));
        List<GroupDto> activeGroups = branchGroups.stream()
                .filter(group -> group.status() == GroupStatus.ACTIVE)
                .toList();
        List<GroupDto> pausedGroups = branchGroups.stream()
                .filter(group -> group.status() == GroupStatus.PAUSED)
                .toList();

        Set<UUID> activeGroupIds = activeGroups.stream().map(GroupDto::groupId).collect(Collectors.toSet());
        Map<UUID, List<GroupCoachDto>> activeCoachesByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> List.copyOf(groupCoachPort.getActiveCoaches(groupId))
                ));
        Map<UUID, Boolean> hasScheduleByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> !groupSchedulePort.getActiveSchedulesByGroup(groupId, resolvedDate).isEmpty()
                ));
        Set<UUID> coachIds = activeCoachesByGroupId.values().stream()
                .flatMap(Collection::stream)
                .map(GroupCoachDto::coachId)
                .collect(Collectors.toSet());

        Map<UUID, CoachDto> coachesById = coachIds.isEmpty()
                ? Map.of()
                : coachPort.getCoaches(coachIds).stream().collect(Collectors.toMap(CoachDto::id, coach -> coach));

        Map<UUID, String> coachNamesById = coachesById.values().stream().collect(Collectors.toMap(CoachDto::id, this::fullName));
        Map<UUID, String> groupNamesById = branchGroups.stream().collect(Collectors.toMap(GroupDto::groupId, GroupDto::name));

        List<CoachSessionAdminView> todaySessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(coachIds, activeGroupIds, resolvedDate, resolvedDate);
        List<CoachSessionAdminView> weekSessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(
                        coachIds,
                        activeGroupIds,
                        resolvedDate.with(DayOfWeek.MONDAY),
                        resolvedDate.with(DayOfWeek.SUNDAY)
                );
        List<CoachSessionAdminView> overdueReports = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getOverdueReportSessions(coachIds, activeGroupIds, resolvedDate);

        DashboardLeadAnalyticsOutput leadAnalytics = analyticsPort.getDashboardLeadAnalytics(branchId, resolvedDate, zoneId.getId());
        List<PaymentOutput> paymentsToday = paymentPort.listPayments(
                new PaymentSearchQuery(
                        branchId,
                        null,
                        null,
                        null,
                        Set.of(PaymentStatus.PAID),
                        null,
                        resolvedDate.atStartOfDay(),
                        resolvedDate.atTime(LocalTime.MAX)
                ),
                Pageable.unpaged()
        ).content();

        Map<UUID, Long> weeklySessionsByCoach = weekSessions.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .collect(Collectors.groupingBy(CoachSessionAdminView::coachId, Collectors.counting()));

        int groupsWithoutCoach = (int) activeGroups.stream()
                .filter(group -> activeCoachesByGroupId.getOrDefault(group.groupId(), List.of()).isEmpty())
                .count();
        int groupsWithoutSchedule = (int) activeGroups.stream()
                .filter(group -> !hasScheduleByGroupId.getOrDefault(group.groupId(), false))
                .count();
        long cancelledToday = todaySessions.stream().filter(session -> "CANCELLED".equals(session.status())).count();
        long activeToday = todaySessions.stream().filter(session -> !"CANCELLED".equals(session.status())).count();
        long trainersOnDuty = todaySessions.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .map(CoachSessionAdminView::coachId)
                .distinct()
                .count();
        long overloadedCoaches = weeklySessionsByCoach.values().stream().filter(count -> count > OVERLOADED_COACH_WEEKLY_LIMIT).count();
        long groupHealthIssues = activeGroups.stream()
                .filter(group -> activeCoachesByGroupId.getOrDefault(group.groupId(), List.of()).isEmpty()
                                 || !hasScheduleByGroupId.getOrDefault(group.groupId(), false))
                .count();

        List<AdminDashboardSessionDto> scheduleItems = todaySessions.stream()
                .sorted(Comparator.comparing(CoachSessionAdminView::scheduledStartAt))
                .map(session -> toSessionDto(session, zoneId, groupNamesById, coachNamesById))
                .toList();
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        AdminDashboardSessionDto nextSession = scheduleItems.stream()
                .filter(item -> !"CANCELLED".equals(item.status()))
                .filter(item -> !item.endAt().isBefore(now))
                .min(Comparator.comparing(AdminDashboardSessionDto::startAt))
                .orElse(null);

        List<AdminDashboardAttentionItemDto> attention = buildAttention(
                overdueReports.size(),
                overloadedCoaches,
                groupsWithoutCoach,
                groupsWithoutSchedule,
                leadAnalytics.slaBreachedLeads()
        );

        return new AdminDashboardSummaryResponse(
                new AdminDashboardMetaDto(
                        branchId,
                        branch.name(),
                        resolvedDate,
                        zoneId.getId(),
                        OffsetDateTime.now(zoneId)
                ),
                new AdminDashboardHeroDto(
                        "Панель администратора",
                        "Экран дня для филиала: что требует внимания, что происходит сегодня и куда перейти следующим действием.",
                        (int) attention.stream().filter(item -> !"success".equals(item.tone()) && !"info".equals(item.tone())).count()
                ),
                attention,
                new AdminDashboardKpisDto(
                        new AdminDashboardKpiItemDto(
                                leadAnalytics.newLeads(),
                                "Новые лиды",
                                "За период с " + resolvedDate.minusDays(27)
                        ),
                        new AdminDashboardKpiItemDto(
                                activeGroups.size(),
                                "Активные группы",
                                pausedGroups.size() + " на паузе"
                        ),
                        new AdminDashboardKpiItemDto(
                                activeToday,
                                "Тренировки сегодня",
                                cancelledToday == 0 ? "Без отмен на сегодня" : cancelledToday + " отмен на сегодня"
                        ),
                        new AdminDashboardKpiItemDto(
                                paymentsToday.size(),
                                "Оплаты за день",
                                paymentsToday.isEmpty() ? "Пока без оплат" : "Поступлений: " + paymentsToday.size()
                        )
                ),
                new AdminDashboardBranchTodayDto(
                        (int) trainersOnDuty,
                        groupsWithoutCoach,
                        groupsWithoutSchedule,
                        leadAnalytics.avgFirstResponseMinutes()
                ),
                List.of(
                        risk("OVERLOADED_COACHES", "Перегруженные тренеры", overloadedCoaches),
                        risk("CANCELLED_TODAY", "Отмены сегодня", cancelledToday),
                        risk("OVERDUE_REPORTS", "Просроченные отчеты", overdueReports.size()),
                        risk("GROUP_HEALTH", "Проблемы по группам", groupHealthIssues)
                ),
                new AdminDashboardLeadFunnelDto(leadAnalytics.funnelTotals()),
                new AdminDashboardTodayScheduleDto(
                        new AdminDashboardTodayScheduleSummaryDto(
                                scheduleItems.size(),
                                (int) activeToday,
                                (int) cancelledToday
                        ),
                        nextSession,
                        scheduleItems
                ),
                new AdminDashboardWeeklyTrendDto(
                        leadAnalytics.weeklyTrend().stream()
                                .map(this::toWeeklyTrendItem)
                                .toList()
                )
        );
    }

    private List<AdminDashboardAttentionItemDto> buildAttention(
            long overdueReports,
            long overloadedCoaches,
            int groupsWithoutCoach,
            int groupsWithoutSchedule,
            long slaBreachedLeads
    ) {
        List<AdminDashboardAttentionItemDto> items = new ArrayList<>();
        if (overdueReports > 0) {
            items.add(new AdminDashboardAttentionItemDto(
                    "overdue-reports",
                    "warning",
                    "Отчеты",
                    overdueReports + " отчетов просрочено",
                    "Напомните тренерам закрыть отчеты по занятиям.",
                    new AdminDashboardActionDto("Проверить тренеров", "/admin/coaches")
            ));
        }
        if (overloadedCoaches > 0) {
            items.add(new AdminDashboardAttentionItemDto(
                    "overloaded-coaches",
                    "danger",
                    "Тренеры",
                    overloadedCoaches + " тренеров перегружено",
                    "Проверьте недельную нагрузку и перераспределите группы.",
                    new AdminDashboardActionDto("Открыть тренеров", "/admin/coaches")
            ));
        }
        if (groupsWithoutCoach > 0 || groupsWithoutSchedule > 0) {
            items.add(new AdminDashboardAttentionItemDto(
                    "group-coverage",
                    "warning",
                    "Группы",
                    "Есть группы без покрытия",
                    "Проверьте назначение тренеров и расписание по активным группам.",
                    new AdminDashboardActionDto("Открыть группы", "/admin/groups")
            ));
        }
        if (slaBreachedLeads > 0) {
            items.add(new AdminDashboardAttentionItemDto(
                    "lead-sla",
                    "danger",
                    "Лиды",
                    slaBreachedLeads + " лидов вне SLA",
                    "Требуется проверить скорость первого контакта по новым обращениям.",
                    new AdminDashboardActionDto("Открыть лиды", "/admin/leads")
            ));
        }
        if (items.isEmpty()) {
            return List.of(new AdminDashboardAttentionItemDto(
                    "all-clear",
                    "success",
                    "Филиал",
                    "Критичных сигналов нет",
                    "По текущим данным операционных блокеров не найдено.",
                    new AdminDashboardActionDto("Открыть лиды", "/admin/leads")
            ));
        }
        return List.copyOf(items);
    }

    private AdminDashboardRiskItemDto risk(String code, String label, long value) {
        return new AdminDashboardRiskItemDto(code, label, value, value > 0 ? "warning" : "success");
    }

    private AdminDashboardSessionDto toSessionDto(
            CoachSessionAdminView session,
            ZoneId zoneId,
            Map<UUID, String> groupNamesById,
            Map<UUID, String> coachNamesById
    ) {
        return new AdminDashboardSessionDto(
                session.sessionId(),
                session.groupId(),
                groupNamesById.getOrDefault(session.groupId(), "Unknown group"),
                session.coachId(),
                coachNamesById.getOrDefault(session.coachId(), "Unknown coach"),
                session.scheduledStartAt().atZone(zoneId).toOffsetDateTime(),
                session.scheduledEndAt().atZone(zoneId).toOffsetDateTime(),
                resolveSessionStatus(session, zoneId),
                resolveScheduleType(session)
        );
    }

    private String resolveScheduleType(CoachSessionAdminView session) {
        if (session.scheduleType() != null && !session.scheduleType().isBlank()) {
            return session.scheduleType();
        }
        return "TEMPORARY";
    }

    private String resolveSessionStatus(CoachSessionAdminView session, ZoneId zoneId) {
        if ("PLANNED".equals(session.status()) && session.scheduledEndAt().isBefore(LocalDateTime.now(zoneId))) {
            return "OVERDUE";
        }
        return session.status();
    }

    private AdminDashboardWeeklyTrendItemDto toWeeklyTrendItem(DashboardLeadWeeklyTrendItemOutput item) {
        return new AdminDashboardWeeklyTrendItemDto(item.bucket(), item.newLeads(), item.wonLeads(), item.lostLeads());
    }

    private String fullName(CoachDto coach) {
        return (Objects.toString(coach.firstName(), "") + " " + Objects.toString(coach.lastName(), "")).trim();
    }

    private ZoneId resolveZone(String timezone) {
        String value = timezone == null || timezone.isBlank() ? DEFAULT_TIMEZONE : timezone.trim();
        return ZoneId.of(value);
    }

    private void verifyAdminAccessToBranch(UUID adminId, UUID branchId, boolean superAdmin) {
        if (superAdmin) {
            branchPort.findById(branchId).orElseThrow(() -> new NotFoundException("Branch not found", branchId));
            return;
        }
        adminService.findById(adminId).orElseThrow(() -> new NotFoundException("Admin not found", adminId));
        if (!adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)) {
            throw new BadRequestException("Admin does not have access to branch", branchId);
        }
    }
}
