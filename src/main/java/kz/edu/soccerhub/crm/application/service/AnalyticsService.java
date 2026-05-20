package kz.edu.soccerhub.crm.application.service;

import kz.edu.soccerhub.common.dto.analytics.*;
import kz.edu.soccerhub.common.port.AnalyticsPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService implements AnalyticsPort {

    private static final String DEFAULT_TIMEZONE = "Asia/Almaty";

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public FunnelAnalyticsOutput getFunnelAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        AnalyticsGroupBy resolvedGroupBy = normalizeGroupBy(groupBy);
        String resolvedTimezone = normalizeTimezone(timezone);

        Map<LeadStatus, Long> totals = loadFunnelTotals(branchId, dateFrom, dateTo, resolvedTimezone);
        List<FunnelSeriesOutput> series = loadFunnelSeries(branchId, dateFrom, dateTo, resolvedGroupBy, resolvedTimezone);

        FunnelRatesOutput rates = buildFunnelRates(totals);
        AnalyticsPeriodOutput period = new AnalyticsPeriodOutput(dateFrom, dateTo, resolvedGroupBy, resolvedTimezone);

        return new FunnelAnalyticsOutput(period, totals, rates, series);
    }

    @Override
    public CoachLoadAnalyticsOutput getCoachLoadAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        AnalyticsPeriodOutput period = new AnalyticsPeriodOutput(
                dateFrom,
                dateTo,
                normalizeGroupBy(groupBy),
                normalizeTimezone(timezone)
        );

        List<CoachLoadRowOutput> rows = loadCoachLoad(branchId, dateFrom, dateTo);
        return new CoachLoadAnalyticsOutput(period, rows);
    }

    @Override
    public RetentionAnalyticsOutput getRetentionAnalytics(
            UUID branchId,
            AnalyticsCohortBy cohortBy,
            int periods,
            String timezone
    ) {
        String resolvedTimezone = normalizeTimezone(timezone);
        List<RetentionCohortOutput> cohorts = loadRetentionCohorts(branchId, periods, resolvedTimezone);
        List<RetentionGroupOutput> groups = loadRetentionGroups(branchId, periods, resolvedTimezone);

        return new RetentionAnalyticsOutput(resolvedTimezone, cohorts, groups);
    }

    @Override
    public SlaAnalyticsOutput getSlaAnalytics(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        AnalyticsPeriodOutput period = new AnalyticsPeriodOutput(
                dateFrom,
                dateTo,
                normalizeGroupBy(groupBy),
                normalizeTimezone(timezone)
        );

        LeadTimingOutput leadTiming = loadLeadTiming(branchId, dateFrom, dateTo, period.timezone());
        SlaBreachesOutput breaches = loadLeadBreaches(branchId, dateFrom, dateTo, period.timezone());

        return new SlaAnalyticsOutput(period, leadTiming, breaches);
    }

    private Map<LeadStatus, Long> loadFunnelTotals(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String timezone
    ) {
        String sql = """
                select status, count(*) as total
                from leads
                where branch_id = :branchId
                  and (created_at at time zone :tz)::date between :dateFrom and :dateTo
                group by status
                """;

        Map<LeadStatus, Long> totals = new EnumMap<>(LeadStatus.class);
        for (LeadStatus status : LeadStatus.values()) {
            totals.put(status, 0L);
        }

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("dateFrom", dateFrom)
                .addValue("dateTo", dateTo)
                .addValue("tz", timezone);

        jdbcTemplate.query(sql, params, rs -> {
            LeadStatus status = LeadStatus.valueOf(rs.getString("status"));
            totals.put(status, rs.getLong("total"));
        });

        return totals;
    }

    private List<FunnelSeriesOutput> loadFunnelSeries(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            AnalyticsGroupBy groupBy,
            String timezone
    ) {
        String bucketExpression = switch (groupBy) {
            case WEEK -> "to_char(date_trunc('week', created_at at time zone :tz), 'IYYY-\"W\"IW')";
            case MONTH -> "to_char(date_trunc('month', created_at at time zone :tz), 'YYYY-MM')";
            case DAY -> "to_char(date_trunc('day', created_at at time zone :tz), 'YYYY-MM-DD')";
        };

        String sql = """
                select %s as bucket,
                       status,
                       count(*) as total
                from leads
                where branch_id = :branchId
                  and (created_at at time zone :tz)::date between :dateFrom and :dateTo
                group by bucket, status
                order by bucket
                """.formatted(bucketExpression);

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("dateFrom", dateFrom)
                .addValue("dateTo", dateTo)
                .addValue("tz", timezone);

        Map<String, Map<LeadStatus, Long>> bucketMap = new HashMap<>();

        jdbcTemplate.query(sql, params, rs -> {
            String bucket = rs.getString("bucket");
            LeadStatus status = LeadStatus.valueOf(rs.getString("status"));
            long total = rs.getLong("total");

            Map<LeadStatus, Long> counts = bucketMap.computeIfAbsent(bucket, key -> {
                Map<LeadStatus, Long> defaults = new EnumMap<>(LeadStatus.class);
                for (LeadStatus leadStatus : LeadStatus.values()) {
                    defaults.put(leadStatus, 0L);
                }
                return defaults;
            });
            counts.put(status, total);
        });

        return bucketMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new FunnelSeriesOutput(entry.getKey(), entry.getValue()))
                .toList();
    }

    private FunnelRatesOutput buildFunnelRates(Map<LeadStatus, Long> totals) {
        long newCount = totals.getOrDefault(LeadStatus.NEW, 0L);
        long qualified = totals.getOrDefault(LeadStatus.QUALIFIED, 0L);
        long trialScheduled = totals.getOrDefault(LeadStatus.TRIAL_SCHEDULED, 0L);
        long won = totals.getOrDefault(LeadStatus.WON, 0L);
        long lost = totals.getOrDefault(LeadStatus.LOST, 0L);

        return new FunnelRatesOutput(
                percent(qualified, newCount),
                percent(trialScheduled, qualified),
                percent(won, trialScheduled),
                percent(won, won + lost)
        );
    }

    private List<CoachLoadRowOutput> loadCoachLoad(UUID branchId, LocalDate dateFrom, LocalDate dateTo) {
        String slotsSql = """
                with schedules as (
                    select gs.coach_id,
                           gs.group_id,
                           gs.status,
                           gs.day_of_week,
                           gs.start_date,
                           gs.end_date
                    from group_schedules gs
                    join groups g on g.id = gs.group_id
                    where g.branch_id = :branchId
                      and gs.start_date <= :dateTo
                      and gs.end_date >= :dateFrom
                ),
                dates as (
                    select d::date as day
                    from generate_series(:dateFrom::date, :dateTo::date, interval '1 day') d
                )
                select s.coach_id,
                       count(*) filter (where s.status = 'CANCELLED') as cancelled_slots,
                       count(*) filter (where s.status <> 'DELETED') as scheduled_slots
                from schedules s
                join dates d on d.day between s.start_date and s.end_date
                where extract(dow from d.day) = case s.day_of_week
                    when 'SUNDAY' then 0
                    when 'MONDAY' then 1
                    when 'TUESDAY' then 2
                    when 'WEDNESDAY' then 3
                    when 'THURSDAY' then 4
                    when 'FRIDAY' then 5
                    when 'SATURDAY' then 6
                end
                group by s.coach_id
                """;

        String groupsSql = """
                select gs.coach_id,
                       count(distinct gs.group_id) as groups_count
                from group_schedules gs
                join groups g on g.id = gs.group_id
                where g.branch_id = :branchId
                  and gs.start_date <= :dateTo
                  and gs.end_date >= :dateFrom
                  and gs.status <> 'DELETED'
                group by gs.coach_id
                """;

        String studentsSql = """
                with coach_groups as (
                    select distinct gs.coach_id, gs.group_id
                    from group_schedules gs
                    join groups g on g.id = gs.group_id
                    where g.branch_id = :branchId
                      and gs.start_date <= :dateTo
                      and gs.end_date >= :dateFrom
                      and gs.status <> 'DELETED'
                )
                select cg.coach_id,
                       count(distinct c.player_id) as active_students
                from coach_groups cg
                join contracts c on c.group_id = cg.group_id
                where c.start_date <= :dateTo
                  and (c.end_date is null or c.end_date >= :dateTo)
                group by cg.coach_id
                """;

        String capacitySql = """
                with coach_groups as (
                    select distinct gs.coach_id, gs.group_id
                    from group_schedules gs
                    join groups g on g.id = gs.group_id
                    where g.branch_id = :branchId
                      and gs.start_date <= :dateTo
                      and gs.end_date >= :dateFrom
                      and gs.status <> 'DELETED'
                )
                select cg.coach_id,
                       coalesce(sum(g.capacity), 0) as target_load
                from coach_groups cg
                join groups g on g.id = cg.group_id
                group by cg.coach_id
                """;

        String coachesSql = """
                select distinct cp.id as coach_id,
                       cp.first_name,
                       cp.last_name
                from coach_profiles cp
                join coach_branches cb on cb.coach_id = cp.id
                where cb.branch_id = :branchId
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("dateFrom", dateFrom)
                .addValue("dateTo", dateTo);

        Map<UUID, CoachLoadRowBuilder> builders = new HashMap<>();

        jdbcTemplate.query(coachesSql, params, rs -> {
            UUID coachId = UUID.fromString(rs.getString("coach_id"));
            String firstName = rs.getString("first_name");
            String lastName = rs.getString("last_name");
            String coachName = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
            builders.put(coachId, new CoachLoadRowBuilder(coachId, coachName.isBlank() ? null : coachName));
        });

        jdbcTemplate.query(groupsSql, params, rs -> {
            UUID coachId = UUID.fromString(rs.getString("coach_id"));
            CoachLoadRowBuilder builder = builders.computeIfAbsent(coachId, CoachLoadRowBuilder::new);
            builder.groups = rs.getInt("groups_count");
        });

        jdbcTemplate.query(slotsSql, params, rs -> {
            UUID coachId = UUID.fromString(rs.getString("coach_id"));
            CoachLoadRowBuilder builder = builders.computeIfAbsent(coachId, CoachLoadRowBuilder::new);
            builder.scheduledSlots = rs.getInt("scheduled_slots");
            builder.cancelledSessions = rs.getInt("cancelled_slots");
        });

        jdbcTemplate.query(studentsSql, params, rs -> {
            UUID coachId = UUID.fromString(rs.getString("coach_id"));
            CoachLoadRowBuilder builder = builders.computeIfAbsent(coachId, CoachLoadRowBuilder::new);
            builder.activeStudents = rs.getInt("active_students");
        });

        jdbcTemplate.query(capacitySql, params, rs -> {
            UUID coachId = UUID.fromString(rs.getString("coach_id"));
            CoachLoadRowBuilder builder = builders.computeIfAbsent(coachId, CoachLoadRowBuilder::new);
            builder.targetLoad = rs.getInt("target_load");
        });

        return builders.values()
                .stream()
                .map(CoachLoadRowBuilder::build)
                .toList();
    }

    private List<RetentionCohortOutput> loadRetentionCohorts(UUID branchId, int periods, String timezone) {
        int effectivePeriods = Math.max(periods, 1);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        LocalDate endMonth = now.toLocalDate().withDayOfMonth(1);
        LocalDate startMonth = endMonth.minusMonths(effectivePeriods - 1L);

        String cohortsSql = """
                select distinct date_trunc('month', c.start_date) as cohort
                from contracts c
                join groups g on g.id = c.group_id
                where g.branch_id = :branchId
                  and c.start_date >= :startMonth
                  and c.start_date < (:endMonth + interval '1 month')
                order by cohort
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("startMonth", startMonth)
                .addValue("endMonth", endMonth);

        List<LocalDate> cohorts = jdbcTemplate.query(cohortsSql, params,
                (rs, rowNum) -> rs.getDate("cohort").toLocalDate());

        List<RetentionCohortOutput> outputs = new ArrayList<>();
        for (LocalDate cohortMonth : cohorts) {
            List<RetentionPointOutput> points = new ArrayList<>();
            int baseCount = countCohortBase(branchId, cohortMonth);

            for (int index = 0; index < effectivePeriods; index++) {
                LocalDate periodStart = cohortMonth.plusMonths(index);
                LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
                int retained = countActiveInCohort(branchId, cohortMonth, periodStart, periodEnd);
                double rate = baseCount == 0 ? 0.0 : (retained * 100.0) / baseCount;
                points.add(new RetentionPointOutput(index, retained, round(rate)));
            }

            outputs.add(new RetentionCohortOutput(formatCohort(cohortMonth), baseCount, points));
        }

        return outputs;
    }

    private int countCohortBase(UUID branchId, LocalDate cohortMonth) {
        String sql = """
                select count(distinct c.player_id) as total
                from contracts c
                join groups g on g.id = c.group_id
                where g.branch_id = :branchId
                  and date_trunc('month', c.start_date) = :cohortMonth
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("cohortMonth", cohortMonth);

        Integer total = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return total == null ? 0 : total;
    }

    private int countActiveInCohort(UUID branchId, LocalDate cohortMonth, LocalDate periodStart, LocalDate periodEnd) {
        String sql = """
                select count(distinct c.player_id) as total
                from contracts c
                join groups g on g.id = c.group_id
                where g.branch_id = :branchId
                  and date_trunc('month', c.start_date) = :cohortMonth
                  and c.start_date <= :periodEnd
                  and (c.end_date is null or c.end_date >= :periodStart)
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("cohortMonth", cohortMonth)
                .addValue("periodStart", periodStart)
                .addValue("periodEnd", periodEnd);

        Integer total = jdbcTemplate.queryForObject(sql, params, Integer.class);
        return total == null ? 0 : total;
    }

    private List<RetentionGroupOutput> loadRetentionGroups(UUID branchId, int periods, String timezone) {
        int effectivePeriods = Math.max(periods, 1);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(timezone));
        LocalDate endDate = now.toLocalDate();
        LocalDate startDate = endDate.minusMonths(effectivePeriods).withDayOfMonth(1);

        String sql = """
                with schedules as (
                    select gs.group_id,
                           gs.status,
                           gs.day_of_week,
                           gs.start_date,
                           gs.end_date
                    from group_schedules gs
                    join groups g on g.id = gs.group_id
                    where g.branch_id = :branchId
                      and gs.start_date <= :dateTo
                      and gs.end_date >= :dateFrom
                ),
                dates as (
                    select d::date as day
                    from generate_series(:dateFrom::date, :dateTo::date, interval '1 day') d
                )
                select s.group_id,
                       count(*) filter (where s.status <> 'DELETED') as total_slots,
                       count(*) filter (where s.status = 'CANCELLED') as cancelled_slots
                from schedules s
                join dates d on d.day between s.start_date and s.end_date
                where extract(dow from d.day) = case s.day_of_week
                    when 'SUNDAY' then 0
                    when 'MONDAY' then 1
                    when 'TUESDAY' then 2
                    when 'WEDNESDAY' then 3
                    when 'THURSDAY' then 4
                    when 'FRIDAY' then 5
                    when 'SATURDAY' then 6
                end
                group by s.group_id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("dateFrom", startDate)
                .addValue("dateTo", endDate);

        Map<UUID, RetentionGroupOutput> groups = new HashMap<>();

        jdbcTemplate.query(sql, params, rs -> {
            UUID groupId = UUID.fromString(rs.getString("group_id"));
            int total = rs.getInt("total_slots");
            int cancelled = rs.getInt("cancelled_slots");
            double retentionIndex = total == 0 ? 0.0 : round(100.0 - (cancelled * 100.0) / total);
            groups.put(groupId, new RetentionGroupOutput(groupId, null, total, cancelled, retentionIndex));
        });

        if (groups.isEmpty()) {
            return List.of();
        }

        String nameSql = """
                select id, name
                from groups
                where id in (:groupIds)
                """;

        MapSqlParameterSource nameParams = new MapSqlParameterSource()
                .addValue("groupIds", new ArrayList<>(groups.keySet()));

        jdbcTemplate.query(nameSql, nameParams, rs -> {
            UUID groupId = UUID.fromString(rs.getString("id"));
            RetentionGroupOutput existing = groups.get(groupId);
            if (existing != null) {
                groups.put(groupId, new RetentionGroupOutput(
                        existing.groupId(),
                        rs.getString("name"),
                        existing.totalSchedules(),
                        existing.cancelled(),
                        existing.retentionIndex()
                ));
            }
        });

        return new ArrayList<>(groups.values());
    }

    private LeadTimingOutput loadLeadTiming(UUID branchId, LocalDate dateFrom, LocalDate dateTo, String timezone) {
        SlaPercentileOutput firstContact = loadPercentiles(
                branchId,
                dateFrom,
                dateTo,
                timezone,
                "CONTACTED",
                ChronoUnit.MINUTES
        );

        SlaPercentileOutput qualification = loadPercentiles(
                branchId,
                dateFrom,
                dateTo,
                timezone,
                "QUALIFIED",
                ChronoUnit.HOURS
        );

        SlaPercentileOutput trialScheduled = loadPercentiles(
                branchId,
                dateFrom,
                dateTo,
                timezone,
                "TRIAL_SCHEDULED",
                ChronoUnit.HOURS
        );

        return new LeadTimingOutput(firstContact, qualification, trialScheduled);
    }

    private SlaBreachesOutput loadLeadBreaches(UUID branchId, LocalDate dateFrom, LocalDate dateTo, String timezone) {
        String firstContactSql = leadDurationSql("CONTACTED");
        String qualificationSql = leadDurationSql("QUALIFIED");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("dateFrom", dateFrom)
                .addValue("dateTo", dateTo)
                .addValue("tz", timezone);

        Double firstContactOver2h = jdbcTemplate.queryForObject(
                """
                        select case when count(*) = 0 then 0
                                    else (count(*) filter (where diff_minutes > 120) * 100.0) / count(*) end
                        from (%s) t
                        """.formatted(firstContactSql),
                params,
                Double.class
        );

        Double qualificationOver48h = jdbcTemplate.queryForObject(
                """
                        select case when count(*) = 0 then 0
                                    else (count(*) filter (where diff_minutes > 2880) * 100.0) / count(*) end
                        from (%s) t
                        """.formatted(qualificationSql),
                params,
                Double.class
        );

        return new SlaBreachesOutput(
                round(firstContactOver2h == null ? 0.0 : firstContactOver2h),
                round(qualificationOver48h == null ? 0.0 : qualificationOver48h)
        );
    }

    private SlaPercentileOutput loadPercentiles(
            UUID branchId,
            LocalDate dateFrom,
            LocalDate dateTo,
            String timezone,
            String status,
            ChronoUnit unit
    ) {
        String sql = """
                select
                    percentile_cont(0.5) within group (order by diff_minutes) as p50,
                    percentile_cont(0.75) within group (order by diff_minutes) as p75,
                    percentile_cont(0.9) within group (order by diff_minutes) as p90
                from (%s) t
                """.formatted(leadDurationSql(status));

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("dateFrom", dateFrom)
                .addValue("dateTo", dateTo)
                .addValue("tz", timezone);

        return jdbcTemplate.query(sql, params, rs -> {
            if (!rs.next()) {
                return new SlaPercentileOutput(0, 0, 0);
            }
            double multiplier = unit == ChronoUnit.HOURS ? 1.0 / 60.0 : 1.0;
            int p50 = (int) Math.round(rs.getDouble("p50") * multiplier);
            int p75 = (int) Math.round(rs.getDouble("p75") * multiplier);
            int p90 = (int) Math.round(rs.getDouble("p90") * multiplier);
            return new SlaPercentileOutput(p50, p75, p90);
        });
    }

    private String leadDurationSql(String targetStatus) {
        return """
                select
                    l.id as lead_id,
                    extract(epoch from (min(a.created_at) - l.created_at)) / 60.0 as diff_minutes
                from leads l
                join lead_activities a on a.lead_id = l.id
                where l.branch_id = :branchId
                  and a.activity_type = 'STATUS_CHANGED'
                  and a.to_status = '%s'
                  and (l.created_at at time zone :tz)::date between :dateFrom and :dateTo
                group by l.id
                """.formatted(targetStatus);
    }

    private String formatCohort(LocalDate cohortMonth) {
        return cohortMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private AnalyticsGroupBy normalizeGroupBy(AnalyticsGroupBy groupBy) {
        return groupBy == null ? AnalyticsGroupBy.DAY : groupBy;
    }

    private String normalizeTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TIMEZONE;
        }
        return timezone;
    }

    private double percent(long numerator, long denominator) {
        if (denominator == 0) {
            return 0.0;
        }
        return round((numerator * 100.0) / denominator);
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static class CoachLoadRowBuilder {
        private final UUID coachId;
        private final String coachName;
        private int groups;
        private int scheduledSlots;
        private int cancelledSessions;
        private int activeStudents;
        private int targetLoad;

        private CoachLoadRowBuilder(UUID coachId) {
            this.coachId = coachId;
            this.coachName = null;
        }

        private CoachLoadRowBuilder(UUID coachId, String coachName) {
            this.coachId = coachId;
            this.coachName = coachName;
        }

        private CoachLoadRowOutput build() {
            int completed = Math.max(0, scheduledSlots - cancelledSessions);
            double utilization = targetLoad == 0 ? 0.0 : round((activeStudents * 100.0) / targetLoad);
            return new CoachLoadRowOutput(
                    coachId,
                    coachName,
                    groups,
                    scheduledSlots,
                    completed,
                    cancelledSessions,
                    activeStudents,
                    targetLoad,
                    utilization
            );
        }
    }
}
