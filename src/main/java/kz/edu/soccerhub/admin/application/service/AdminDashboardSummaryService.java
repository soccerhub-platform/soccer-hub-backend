package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.admin.application.dto.dashboard.*;
import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.analytics.AnalyticsResponseOutput;
import kz.edu.soccerhub.common.dto.analytics.DashboardLeadAnalyticsOutput;
import kz.edu.soccerhub.common.dto.branch.BranchDto;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.coach.CoachDto;
import kz.edu.soccerhub.common.dto.coach.CoachSessionAdminView;
import kz.edu.soccerhub.common.dto.coach.PlayerAttendanceRateDto;
import kz.edu.soccerhub.common.dto.coach.SessionAttendanceSummaryDto;
import kz.edu.soccerhub.common.dto.contract.StudentContractSnapshotOutput;
import kz.edu.soccerhub.common.dto.group.GroupCoachDto;
import kz.edu.soccerhub.common.dto.group.GroupDto;
import kz.edu.soccerhub.common.dto.payment.PaymentOutput;
import kz.edu.soccerhub.common.dto.payment.PaymentSearchQuery;
import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.common.exception.NotFoundException;
import kz.edu.soccerhub.common.port.*;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.organization.domain.model.enums.GroupStatus;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardSummaryService {

    private static final int OVERLOADED_COACH_WEEKLY_LIMIT = 12;
    private static final int MAX_TOP_ALERT_CARDS = 3;
    private static final int MAX_ATTENTION_ITEMS = 7;
    private static final int ENDING_SOON_DAYS = 7;
    private static final List<LeadStatus> FUNNEL_STATUSES = List.of(
            LeadStatus.NEW,
            LeadStatus.IN_PROGRESS,
            LeadStatus.TRIAL_SCHEDULED,
            LeadStatus.DECISION_PENDING,
            LeadStatus.CONVERTED
    );

    private final AdminService adminService;
    private final AdminBranchService adminBranchService;
    private final BranchPort branchPort;
    private final GroupPort groupPort;
    private final GroupCoachPort groupCoachPort;
    private final GroupSchedulePort groupSchedulePort;
    private final CoachPort coachPort;
    private final ClientPort clientPort;
    private final ContractPort contractPort;
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

        BranchDto branch = branchPort.findById(branchId).orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        ZoneId zoneId = resolveZone(timezone, branch.timezone());
        LocalDate resolvedDate = date;

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
        int expiringContractsSoon = countExpiringContractsSoon(branchId, resolvedDate);

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

        List<AdminDashboardAttentionItemDto> attention = buildAttention(
                overdueReports.size(),
                overloadedCoaches,
                groupsWithoutCoach,
                groupsWithoutSchedule,
                leadAnalytics.slaBreachedLeads(),
                expiringContractsSoon
        );

        return new AdminDashboardSummaryResponse(
                new AdminDashboardMetaDto(
                        branchId,
                        branch.name(),
                        resolvedDate,
                        zoneId.getId(),
                        OffsetDateTime.now(zoneId)
                ),
                new AdminDashboardAlertsDto(
                        buildTopCards(attention),
                        attention
                ),
                buildKpis(
                        branchId,
                        resolvedDate,
                        zoneId,
                        activeGroups.size(),
                        activeToday,
                        cancelledToday,
                        coachIds,
                        activeGroupIds,
                        paymentsToday
                ),
                buildBranchSummary(
                        branchId,
                        resolvedDate,
                        zoneId,
                        todaySessions,
                        trainersOnDuty,
                        groupsWithoutCoach,
                        groupsWithoutSchedule,
                        leadAnalytics.avgFirstResponseMinutes()
                ),
                buildRisks(
                        branchId,
                        activeGroups,
                        activeCoachesByGroupId,
                        hasScheduleByGroupId,
                        overdueReports,
                        cancelledToday,
                        overloadedCoaches,
                        expiringContractsSoon
                ),
                buildFunnel(leadAnalytics.funnelTotals()),
                buildTodaySchedule(todaySessions, zoneId, groupNamesById, coachNamesById),
                buildWeeklyDynamics(
                        branchId,
                        resolvedDate.minusDays(6),
                        resolvedDate,
                        zoneId,
                        coachIds,
                        activeGroupIds
                )
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardTodayScheduleDto getTodaySchedule(
            UUID adminId,
            UUID branchId,
            LocalDate date,
            String timezone,
            boolean superAdmin
    ) {
        verifyAdminAccessToBranch(adminId, branchId, superAdmin);

        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        ZoneId zoneId = resolveZone(timezone, branch.timezone());
        List<GroupDto> activeGroups = groupPort.getGroupsByBranch(branchId).stream()
                .filter(group -> group.status() == GroupStatus.ACTIVE)
                .toList();
        Set<UUID> activeGroupIds = activeGroups.stream()
                .map(GroupDto::groupId)
                .collect(Collectors.toSet());
        Map<UUID, String> groupNamesById = activeGroups.stream()
                .collect(Collectors.toMap(GroupDto::groupId, GroupDto::name));

        Map<UUID, List<GroupCoachDto>> activeCoachesByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> List.copyOf(groupCoachPort.getActiveCoaches(groupId))
                ));
        Set<UUID> coachIds = activeCoachesByGroupId.values().stream()
                .flatMap(Collection::stream)
                .map(GroupCoachDto::coachId)
                .collect(Collectors.toSet());
        Map<UUID, String> coachNamesById = coachIds.isEmpty()
                ? Map.of()
                : coachPort.getCoaches(coachIds).stream()
                        .collect(Collectors.toMap(CoachDto::id, this::fullName));

        List<CoachSessionAdminView> todaySessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(coachIds, activeGroupIds, date, date);

        return buildTodaySchedule(todaySessions, zoneId, groupNamesById, coachNamesById);
    }

    @Transactional(readOnly = true)
    public AdminDashboardBranchTodayDto getBranchSummary(
            UUID adminId,
            UUID branchId,
            LocalDate date,
            String timezone,
            boolean superAdmin
    ) {
        verifyAdminAccessToBranch(adminId, branchId, superAdmin);

        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        ZoneId zoneId = resolveZone(timezone, branch.timezone());
        List<GroupDto> activeGroups = groupPort.getGroupsByBranch(branchId).stream()
                .filter(group -> group.status() == GroupStatus.ACTIVE)
                .toList();
        Set<UUID> activeGroupIds = activeGroups.stream()
                .map(GroupDto::groupId)
                .collect(Collectors.toSet());
        Map<UUID, List<GroupCoachDto>> activeCoachesByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> List.copyOf(groupCoachPort.getActiveCoaches(groupId))
                ));
        Map<UUID, Boolean> hasScheduleByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> !groupSchedulePort.getActiveSchedulesByGroup(groupId, date).isEmpty()
                ));
        Set<UUID> coachIds = activeCoachesByGroupId.values().stream()
                .flatMap(Collection::stream)
                .map(GroupCoachDto::coachId)
                .collect(Collectors.toSet());
        List<CoachSessionAdminView> todaySessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(coachIds, activeGroupIds, date, date);
        DashboardLeadAnalyticsOutput leadAnalytics = analyticsPort.getDashboardLeadAnalytics(branchId, date, zoneId.getId());

        int groupsWithoutCoach = (int) activeGroups.stream()
                .filter(group -> activeCoachesByGroupId.getOrDefault(group.groupId(), List.of()).isEmpty())
                .count();
        int groupsWithoutSchedule = (int) activeGroups.stream()
                .filter(group -> !hasScheduleByGroupId.getOrDefault(group.groupId(), false))
                .count();
        long trainersOnDuty = todaySessions.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .map(CoachSessionAdminView::coachId)
                .distinct()
                .count();

        return buildBranchSummary(
                branchId,
                date,
                zoneId,
                todaySessions,
                trainersOnDuty,
                groupsWithoutCoach,
                groupsWithoutSchedule,
                leadAnalytics.avgFirstResponseMinutes()
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardRisksDto getRisks(
            UUID adminId,
            UUID branchId,
            LocalDate date,
            String timezone,
            boolean superAdmin
    ) {
        verifyAdminAccessToBranch(adminId, branchId, superAdmin);

        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        ZoneId zoneId = resolveZone(timezone, branch.timezone());
        List<GroupDto> activeGroups = groupPort.getGroupsByBranch(branchId).stream()
                .filter(group -> group.status() == GroupStatus.ACTIVE)
                .toList();
        Set<UUID> activeGroupIds = activeGroups.stream()
                .map(GroupDto::groupId)
                .collect(Collectors.toSet());
        Map<UUID, List<GroupCoachDto>> activeCoachesByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> List.copyOf(groupCoachPort.getActiveCoaches(groupId))
                ));
        Map<UUID, Boolean> hasScheduleByGroupId = activeGroupIds.stream()
                .collect(Collectors.toMap(
                        groupId -> groupId,
                        groupId -> !groupSchedulePort.getActiveSchedulesByGroup(groupId, date).isEmpty()
                ));
        Set<UUID> coachIds = activeCoachesByGroupId.values().stream()
                .flatMap(Collection::stream)
                .map(GroupCoachDto::coachId)
                .collect(Collectors.toSet());
        List<CoachSessionAdminView> todaySessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(coachIds, activeGroupIds, date, date);
        List<CoachSessionAdminView> weekSessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(
                        coachIds,
                        activeGroupIds,
                        date.with(DayOfWeek.MONDAY),
                        date.with(DayOfWeek.SUNDAY)
                );
        List<CoachSessionAdminView> overdueReports = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getOverdueReportSessions(coachIds, activeGroupIds, date);

        Map<UUID, Long> weeklySessionsByCoach = weekSessions.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .collect(Collectors.groupingBy(CoachSessionAdminView::coachId, Collectors.counting()));
        long cancelledToday = todaySessions.stream().filter(session -> "CANCELLED".equals(session.status())).count();
        long overloadedCoaches = weeklySessionsByCoach.values().stream().filter(count -> count > OVERLOADED_COACH_WEEKLY_LIMIT).count();
        int expiringContractsSoon = countExpiringContractsSoon(branchId, date);

        return buildRisks(
                branchId,
                activeGroups,
                activeCoachesByGroupId,
                hasScheduleByGroupId,
                overdueReports,
                cancelledToday,
                overloadedCoaches,
                expiringContractsSoon
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardWeeklyTrendDto getWeeklyDynamics(
            UUID adminId,
            UUID branchId,
            LocalDate from,
            LocalDate to,
            LocalDate date,
            String timezone,
            boolean superAdmin
    ) {
        verifyAdminAccessToBranch(adminId, branchId, superAdmin);

        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        ZoneId zoneId = resolveZone(timezone, branch.timezone());
        LocalDate resolvedDate = date == null ? LocalDate.now(zoneId) : date;
        LocalDate dateFrom = from == null ? resolvedDate.minusDays(6) : from;
        LocalDate dateTo = to == null ? resolvedDate : to;

        List<GroupDto> activeGroups = groupPort.getGroupsByBranch(branchId).stream()
                .filter(group -> group.status() == GroupStatus.ACTIVE)
                .toList();
        Set<UUID> activeGroupIds = activeGroups.stream()
                .map(GroupDto::groupId)
                .collect(Collectors.toSet());
        Set<UUID> coachIds = activeGroupIds.stream()
                .flatMap(groupId -> groupCoachPort.getActiveCoaches(groupId).stream())
                .map(GroupCoachDto::coachId)
                .collect(Collectors.toSet());

        return buildWeeklyDynamics(branchId, dateFrom, dateTo, zoneId, coachIds, activeGroupIds);
    }

    @Transactional(readOnly = true)
    public AdminDashboardLeadFunnelDto getFunnel(
            UUID adminId,
            UUID branchId,
            LocalDate from,
            LocalDate to,
            LocalDate date,
            String timezone,
            boolean superAdmin
    ) {
        verifyAdminAccessToBranch(adminId, branchId, superAdmin);

        BranchDto branch = branchPort.findById(branchId)
                .orElseThrow(() -> new NotFoundException("Branch not found", branchId));
        ZoneId zoneId = resolveZone(timezone, branch.timezone());
        LocalDate resolvedDate = date == null ? LocalDate.now(zoneId) : date;
        LocalDate dateFrom = from == null ? resolvedDate.minusDays(27) : from;
        LocalDate dateTo = to == null ? resolvedDate : to;

        AnalyticsResponseOutput response = analyticsPort.getFunnelAnalytics(
                branchId,
                dateFrom,
                dateTo,
                null,
                zoneId.getId(),
                null,
                null
        );

        return buildFunnel(extractFunnelTotals(response.totals()));
    }

    private List<AdminDashboardAttentionItemDto> buildAttention(
            long overdueReports,
            long overloadedCoaches,
            int groupsWithoutCoach,
            int groupsWithoutSchedule,
            long slaBreachedLeads,
            int expiringContractsSoon
    ) {
        List<AdminDashboardAttentionItemDto> items = new ArrayList<>();
        if (slaBreachedLeads > 0) {
            items.add(new AdminDashboardAttentionItemDto(
                    "waiting-leads",
                    "danger",
                    "Лиды",
                    slaBreachedLeads + " лида ждут ответа",
                    "Проверьте лиды с нарушением SLA первого контакта.",
                    new AdminDashboardActionDto("Перейти к лидам", "/admin/leads?filter=waiting-response")
            ));
        }
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
        if (expiringContractsSoon > 0) {
            items.add(new AdminDashboardAttentionItemDto(
                    "contracts-ending-soon",
                    "warning",
                    "Договоры",
                    expiringContractsSoon + " договоров скоро истекают",
                    "Проверьте продление активных договоров на ближайшие дни.",
                    new AdminDashboardActionDto("Открыть договоры", "/admin/contracts?filter=ending-soon")
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
        return List.copyOf(items.stream().limit(MAX_ATTENTION_ITEMS).toList());
    }

    private List<AdminDashboardAlertCardDto> buildTopCards(List<AdminDashboardAttentionItemDto> attention) {
        return attention.stream()
                .filter(item -> !"success".equals(item.tone()))
                .limit(MAX_TOP_ALERT_CARDS)
                .map(item -> new AdminDashboardAlertCardDto(
                        item.id(),
                        item.tone(),
                        resolveAlertIcon(item),
                        item.title(),
                        item.description(),
                        item.action(),
                        buildAlertDetails(item)
                ))
                .toList();
    }

    private AdminDashboardKpisDto buildKpis(
            UUID branchId,
            LocalDate date,
            ZoneId zoneId,
            int activeGroups,
            long activeToday,
            long cancelledToday,
            Set<UUID> coachIds,
            Set<UUID> activeGroupIds,
            List<PaymentOutput> paymentsToday
    ) {
        LocalDate previousDate = date.minusDays(1);

        long newLeadsToday = analyticsPort.countCreatedLeads(branchId, date, zoneId.getId());
        long newLeadsYesterday = analyticsPort.countCreatedLeads(branchId, previousDate, zoneId.getId());

        long trainingsYesterday = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? 0
                : coachPort.getSessions(coachIds, activeGroupIds, previousDate, previousDate).stream()
                        .filter(session -> !"CANCELLED".equals(session.status()))
                        .count();

        List<PaymentOutput> paymentsYesterday = paymentPort.listPayments(
                new PaymentSearchQuery(
                        branchId,
                        null,
                        null,
                        null,
                        Set.of(PaymentStatus.PAID),
                        null,
                        previousDate.atStartOfDay(),
                        previousDate.atTime(LocalTime.MAX)
                ),
                Pageable.unpaged()
        ).content();

        BigDecimal paymentAmountToday = sumAmounts(paymentsToday);
        BigDecimal paymentAmountYesterday = sumAmounts(paymentsYesterday);

        return new AdminDashboardKpisDto(List.of(
                new AdminDashboardKpiItemDto(
                        "newLeads",
                        "Новые лиды",
                        newLeadsToday,
                        Long.toString(newLeadsToday),
                        buildDelta(newLeadsToday, newLeadsYesterday, "count"),
                        "vs вчера (" + newLeadsYesterday + ")",
                        "/admin/leads",
                        null,
                        null
                ),
                new AdminDashboardKpiItemDto(
                        "activeGroups",
                        "Активные группы",
                        activeGroups,
                        Integer.toString(activeGroups),
                        new AdminDashboardKpiDeltaDto(0, "count", "info", "0"),
                        "Текущий снимок филиала",
                        "/admin/groups",
                        null,
                        null
                ),
                new AdminDashboardKpiItemDto(
                        "trainingsToday",
                        "Тренировки сегодня",
                        activeToday,
                        Long.toString(activeToday),
                        buildDelta(activeToday, trainingsYesterday, "count"),
                        "vs вчера (" + trainingsYesterday + ")" + (cancelledToday > 0 ? ", отменено " + cancelledToday : ""),
                        "/admin/dashboard/today-schedule",
                        null,
                        null
                ),
                new AdminDashboardKpiItemDto(
                        "paymentsToday",
                        "Оплаты за день",
                        paymentsToday.size(),
                        Integer.toString(paymentsToday.size()),
                        buildAmountDelta(paymentAmountToday, paymentAmountYesterday),
                        "vs вчера (" + paymentsYesterday.size() + "), сумма " + paymentAmountToday.toPlainString() + " KZT",
                        "/admin/payments",
                        (long) paymentsToday.size(),
                        paymentAmountToday
                )
        ));
    }

    private AdminDashboardKpiDeltaDto buildDelta(long current, long previous, String unit) {
        long delta = current - previous;
        String tone = delta > 0 ? "success" : delta < 0 ? "warning" : "info";
        String label = (delta > 0 ? "+" : "") + delta;
        return new AdminDashboardKpiDeltaDto(delta, unit, tone, label);
    }

    private AdminDashboardKpiDeltaDto buildAmountDelta(BigDecimal current, BigDecimal previous) {
        BigDecimal delta = current.subtract(previous);
        int sign = delta.signum();
        String tone = sign > 0 ? "success" : sign < 0 ? "warning" : "info";
        String label = (sign > 0 ? "+" : "") + delta.toPlainString();
        return new AdminDashboardKpiDeltaDto(delta.longValue(), "amount", tone, label);
    }

    private AdminDashboardTodayScheduleDto buildTodaySchedule(
            List<CoachSessionAdminView> todaySessions,
            ZoneId zoneId,
            Map<UUID, String> groupNamesById,
            Map<UUID, String> coachNamesById
    ) {
        List<AdminDashboardSessionDto> scheduleItems = todaySessions.stream()
                .sorted(Comparator.comparing(CoachSessionAdminView::scheduledStartAt))
                .map(session -> toSessionDto(session, zoneId, groupNamesById, coachNamesById))
                .toList();

        int cancelled = (int) todaySessions.stream()
                .filter(session -> "CANCELLED".equals(session.status()))
                .count();
        int active = todaySessions.size() - cancelled;
        OffsetDateTime now = OffsetDateTime.now(zoneId);
        AdminDashboardSessionDto nextSession = scheduleItems.stream()
                .filter(item -> !"CANCELLED".equals(item.status()))
                .filter(item -> !item.endAt().isBefore(now))
                .min(Comparator.comparing(AdminDashboardSessionDto::startAt))
                .orElse(null);

        return new AdminDashboardTodayScheduleDto(
                new AdminDashboardTodayScheduleSummaryDto(scheduleItems.size(), active, cancelled),
                nextSession,
                scheduleItems
        );
    }

    private AdminDashboardBranchTodayDto buildBranchSummary(
            UUID branchId,
            LocalDate date,
            ZoneId zoneId,
            List<CoachSessionAdminView> todaySessions,
            long trainersOnDuty,
            int groupsWithoutCoach,
            int groupsWithoutSchedule,
            int avgFirstResponseMinutes
    ) {
        Map<String, String> unavailableReasons = new LinkedHashMap<>();

        long studentsTotal = clientPort.countStudentsAsOf(branchId, date, zoneId.getId());
        long studentsPreviousTotal = clientPort.countStudentsAsOf(branchId, date.minusDays(1), zoneId.getId());
        long newStudents = clientPort.countCreatedStudents(branchId, date, zoneId.getId());
        long newStudentsYesterday = clientPort.countCreatedStudents(branchId, date.minusDays(1), zoneId.getId());

        List<CoachSessionAdminView> activeSessions = todaySessions.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .toList();
        long trainingsTotal = activeSessions.size();

        Long trainingsVisited = null;
        Integer attendancePercent = null;
        if (trainingsTotal == 0) {
            unavailableReasons.put("attendancePercent", "No active sessions for selected date");
        } else {
            Set<UUID> sessionIds = activeSessions.stream()
                    .map(CoachSessionAdminView::sessionId)
                    .collect(Collectors.toSet());
            List<SessionAttendanceSummaryDto> attendanceSummaries = coachPort.getSessionAttendanceSummaries(sessionIds);
            boolean hasAttendanceData = attendanceSummaries.stream().anyMatch(item -> item.totalMarked() > 0);
            if (hasAttendanceData) {
                trainingsVisited = attendanceSummaries.stream()
                        .filter(item -> item.presentLikeMarked() > 0)
                        .count();
                attendancePercent = percentage(trainingsVisited, trainingsTotal);
            } else {
                unavailableReasons.put("trainingsVisited", "Attendance has not been recorded for selected date");
                unavailableReasons.put("attendancePercent", "Attendance has not been recorded for selected date");
            }
        }

        return new AdminDashboardBranchTodayDto(
                studentsTotal,
                studentsTotal - studentsPreviousTotal,
                trainingsVisited,
                trainingsTotal,
                attendancePercent,
                newStudents,
                newStudents - newStudentsYesterday,
                (int) trainersOnDuty,
                groupsWithoutCoach,
                groupsWithoutSchedule,
                avgFirstResponseMinutes,
                Map.copyOf(unavailableReasons)
        );
    }

    private AdminDashboardLeadFunnelDto buildFunnel(Map<LeadStatus, Long> totals) {
        long newLeads = totals.getOrDefault(LeadStatus.NEW, 0L);
        List<AdminDashboardLeadFunnelRowDto> rows = FUNNEL_STATUSES.stream()
                .map(status -> {
                    long count = totals.getOrDefault(status, 0L);
                    return new AdminDashboardLeadFunnelRowDto(
                            status,
                            funnelLabel(status),
                            count,
                            percentage(count, newLeads)
                    );
                })
                .toList();

        return new AdminDashboardLeadFunnelDto(
                rows,
                percentage(totals.getOrDefault(LeadStatus.CONVERTED, 0L), newLeads)
        );
    }

    private Map<LeadStatus, Long> extractFunnelTotals(Object totals) {
        if (!(totals instanceof Map<?, ?> rawTotals)) {
            return Map.of();
        }

        Map<LeadStatus, Long> result = new EnumMap<>(LeadStatus.class);
        rawTotals.forEach((key, value) -> {
            LeadStatus status = null;
            if (key instanceof LeadStatus leadStatus) {
                status = leadStatus;
            } else if (key instanceof String rawStatus) {
                try {
                    status = LeadStatus.valueOf(rawStatus);
                } catch (IllegalArgumentException ignored) {
                    return;
                }
            }

            if (status == null || !(value instanceof Number number)) {
                return;
            }

            result.put(status, number.longValue());
        });
        return result;
    }

    private String funnelLabel(LeadStatus status) {
        return switch (status) {
            case NEW -> "Новый";
            case IN_PROGRESS -> "В работе";
            case TRIAL_SCHEDULED -> "Пробная назначена";
            case DECISION_PENDING -> "Ожидают решения";
            case CONVERTED -> "Клиент";
            default -> status.name();
        };
    }

    private int percentage(long value, long total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((double) value * 100 / total);
    }

    private BigDecimal sumAmounts(List<PaymentOutput> payments) {
        return payments.stream()
                .map(PaymentOutput::amount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String resolveAlertIcon(AdminDashboardAttentionItemDto item) {
        return switch (item.id()) {
            case "lead-sla" -> "leads";
            case "overloaded-coaches" -> "coach";
            case "group-coverage" -> "groups";
            case "overdue-reports" -> "reports";
            default -> "info";
        };
    }

    private List<AdminDashboardAlertCardDetailDto> buildAlertDetails(AdminDashboardAttentionItemDto item) {
        if ("waiting-leads".equals(item.id())) {
            return List.of(new AdminDashboardAlertCardDetailDto("SLA", "2+ hours"));
        }
        return List.of();
    }

    private int countExpiringContractsSoon(UUID branchId, LocalDate date) {
        List<UUID> playerIds = clientPort.getStudentProfilesByBranch(branchId).stream()
                .map(kz.edu.soccerhub.common.dto.student.StudentProfileDto::playerId)
                .toList();
        if (playerIds.isEmpty()) {
            return 0;
        }
        return (int) contractPort.getStudentContracts(branchId, playerIds).stream()
                .filter(contract -> contract.status() == ContractStatus.ACTIVE)
                .filter(contract -> contract.endDate() != null)
                .filter(contract -> !contract.endDate().isBefore(date))
                .filter(contract -> !contract.endDate().isAfter(date.plusDays(ENDING_SOON_DAYS)))
                .map(StudentContractSnapshotOutput::id)
                .distinct()
                .count();
    }

    private AdminDashboardRisksDto buildRisks(
            UUID branchId,
            List<GroupDto> activeGroups,
            Map<UUID, List<GroupCoachDto>> activeCoachesByGroupId,
            Map<UUID, Boolean> hasScheduleByGroupId,
            List<CoachSessionAdminView> overdueReports,
            long cancelledToday,
            long overloadedCoaches,
            int expiringContractsSoon
    ) {
        List<RankedRisk> risks = new ArrayList<>();

        for (GroupDto group : activeGroups) {
            List<GroupMemberDto> members = clientPort.getGroupMembers(group.groupId());
            if (members.isEmpty()) {
                continue;
            }
            Set<UUID> playerIds = members.stream()
                    .map(GroupMemberDto::playerId)
                    .collect(Collectors.toSet());
            List<PlayerAttendanceRateDto> attendanceRates = coachPort.getAttendanceRates(group.groupId(), playerIds);
            if (attendanceRates.isEmpty()) {
                continue;
            }
            int averageAttendance = (int) Math.round(attendanceRates.stream()
                    .mapToInt(PlayerAttendanceRateDto::attendanceRate)
                    .average()
                    .orElse(0));
            if (averageAttendance < 60) {
                risks.add(rankedRisk(
                        0,
                        0,
                        "low-attendance",
                        "Низкая посещаемость в группе " + group.name(),
                        "Средняя посещаемость: " + averageAttendance + "%",
                        averageAttendance,
                        "percent",
                        "danger",
                        "/admin/groups/" + group.groupId()
                ));
            }
        }

        if (expiringContractsSoon > 0) {
            risks.add(rankedRisk(
                    1,
                    1,
                    "contracts-ending-soon",
                    "Скоро истекают договоры",
                    expiringContractsSoon + " договоров истекают в ближайшие 7 дней",
                    expiringContractsSoon,
                    "count",
                    "warning",
                    "/admin/contracts?filter=ending-soon"
            ));
        }
        if (!overdueReports.isEmpty()) {
            risks.add(rankedRisk(
                    0,
                    2,
                    "overdue-reports",
                    "Просроченные отчеты тренеров",
                    overdueReports.size() + " тренировок без отчета",
                    overdueReports.size(),
                    "count",
                    "danger",
                    "/admin/coaches"
            ));
        }

        long groupsWithoutCoach = activeGroups.stream()
                .filter(group -> activeCoachesByGroupId.getOrDefault(group.groupId(), List.of()).isEmpty())
                .count();
        if (groupsWithoutCoach > 0) {
            risks.add(rankedRisk(
                    0,
                    3,
                    "groups-without-coach",
                    "Группы без тренера",
                    groupsWithoutCoach + " активных групп без назначенного тренера",
                    groupsWithoutCoach,
                    "count",
                    "danger",
                    "/admin/groups"
            ));
        }

        long groupsWithoutSchedule = activeGroups.stream()
                .filter(group -> !hasScheduleByGroupId.getOrDefault(group.groupId(), false))
                .count();
        if (groupsWithoutSchedule > 0) {
            risks.add(rankedRisk(
                    1,
                    4,
                    "groups-without-schedule",
                    "Группы без расписания",
                    groupsWithoutSchedule + " активных групп без активного расписания",
                    groupsWithoutSchedule,
                    "count",
                    "warning",
                    "/admin/groups"
            ));
        }

        if (overloadedCoaches > 0) {
            risks.add(rankedRisk(
                    1,
                    5,
                    "coach-overload",
                    "Перегруженные тренеры",
                    overloadedCoaches + " тренеров выше недельного лимита",
                    overloadedCoaches,
                    "count",
                    "warning",
                    "/admin/coaches"
            ));
        }

        if (cancelledToday > 0) {
            risks.add(rankedRisk(
                    1,
                    6,
                    "today-cancellations",
                    "Отмены на сегодня",
                    cancelledToday + " тренировок отменено на выбранную дату",
                    cancelledToday,
                    "count",
                    "warning",
                    "/admin/dashboard/today-schedule?branchId=" + branchId
            ));
        }

        return new AdminDashboardRisksDto(
                risks.stream()
                        .sorted(Comparator
                                .comparingInt(RankedRisk::severityRank)
                                .thenComparingInt(RankedRisk::priorityRank))
                        .map(RankedRisk::item)
                        .toList()
        );
    }

    private RankedRisk rankedRisk(
            int severityRank,
            int priorityRank,
            String code,
            String label,
            String description,
            long value,
            String unit,
            String tone,
            String target
    ) {
        return new RankedRisk(
                severityRank,
                priorityRank,
                new AdminDashboardRiskItemDto(code, label, description, value, unit, tone, target)
        );
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
        if ("PLANNED".equals(session.status()) && session.scheduledEndAt().isBefore(java.time.LocalDateTime.now(zoneId))) {
            return "OVERDUE";
        }
        return session.status();
    }

    private AdminDashboardWeeklyTrendDto buildWeeklyDynamics(
            UUID branchId,
            LocalDate from,
            LocalDate to,
            ZoneId zoneId,
            Set<UUID> coachIds,
            Set<UUID> activeGroupIds
    ) {
        List<LocalDate> dates = from.datesUntil(to.plusDays(1)).toList();
        Map<LocalDate, BigDecimal> leadCountsByDate = dates.stream()
                .collect(Collectors.toMap(
                        Function.identity(),
                        day -> BigDecimal.valueOf(analyticsPort.countCreatedLeads(branchId, day, zoneId.getId())),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<CoachSessionAdminView> sessions = coachIds.isEmpty() || activeGroupIds.isEmpty()
                ? List.of()
                : coachPort.getSessions(coachIds, activeGroupIds, from, to);
        Map<LocalDate, BigDecimal> trainingsByDate = sessions.stream()
                .filter(session -> !"CANCELLED".equals(session.status()))
                .collect(Collectors.groupingBy(
                        CoachSessionAdminView::sessionDate,
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, item -> BigDecimal.ONE, BigDecimal::add)
                ));

        List<PaymentOutput> payments = paymentPort.listPayments(
                new PaymentSearchQuery(
                        branchId,
                        null,
                        null,
                        null,
                        Set.of(PaymentStatus.PAID),
                        null,
                        from.atStartOfDay(),
                        to.atTime(LocalTime.MAX)
                ),
                Pageable.unpaged()
        ).content();
        Map<LocalDate, BigDecimal> paymentsByDate = payments.stream()
                .filter(payment -> payment.paidAt() != null)
                .collect(Collectors.groupingBy(
                        payment -> payment.paidAt().atZone(zoneId).toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, PaymentOutput::amount, BigDecimal::add)
                ));

        List<AdminDashboardSeriesDto> series = List.of(
                buildSeries("leads", "Лиды", "count", dates, leadCountsByDate),
                buildSeries("trainings", "Тренировки", "count", dates, trainingsByDate),
                buildSeries("payments", "Оплаты", "amount", dates, paymentsByDate)
        );

        boolean isEmpty = series.stream()
                .flatMap(item -> item.points().stream())
                .allMatch(point -> point.value().compareTo(BigDecimal.ZERO) == 0);

        return new AdminDashboardWeeklyTrendDto(
                new AdminDashboardWeeklyTrendPeriodDto(from, to),
                series,
                isEmpty,
                isEmpty ? "No activity for the selected period" : null
        );
    }

    private AdminDashboardSeriesDto buildSeries(
            String code,
            String label,
            String unit,
            List<LocalDate> dates,
            Map<LocalDate, BigDecimal> valuesByDate
    ) {
        return new AdminDashboardSeriesDto(
                code,
                label,
                unit,
                dates.stream()
                        .map(day -> new AdminDashboardSeriesPointDto(day, valuesByDate.getOrDefault(day, BigDecimal.ZERO)))
                        .toList()
        );
    }

    private String fullName(CoachDto coach) {
        return (Objects.toString(coach.firstName(), "") + " " + Objects.toString(coach.lastName(), "")).trim();
    }

    private ZoneId resolveZone(String timezone, String branchTimezone) {
        String value = timezone;
        if (value == null || value.isBlank()) {
            value = branchTimezone;
        }
        if (value == null || value.isBlank()) {
            value = "Asia/Almaty";
        }
        return ZoneId.of(value);
    }

    private record RankedRisk(
            int severityRank,
            int priorityRank,
            AdminDashboardRiskItemDto item
    ) {
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
