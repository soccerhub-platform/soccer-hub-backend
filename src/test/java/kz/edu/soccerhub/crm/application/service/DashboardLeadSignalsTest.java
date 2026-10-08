package kz.edu.soccerhub.crm.application.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import java.time.LocalDate;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardLeadSignalsTest {
    @Test
    void overdueTasksAreScopedAndExcludeClosedLeads() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var service = new AnalyticsService(jdbc);
        var branch = UUID.randomUUID();
        when(jdbc.queryForObject(anyString(), any(SqlParameterSource.class), eq(Long.class))).thenReturn(4L);
        assertEquals(4, service.countOverdueLeadTasks(branch));
        var sql = ArgumentCaptor.forClass(String.class);
        var params = ArgumentCaptor.forClass(SqlParameterSource.class);
        verify(jdbc).queryForObject(sql.capture(), params.capture(), eq(Long.class));
        assertEquals(branch, params.getValue().getValue("branchId"));
        assertTrue(sql.getValue().contains("l.branch_id = :branchId"));
        assertTrue(sql.getValue().contains("l.status not in ('CONVERTED', 'LOST')"));
        assertTrue(sql.getValue().contains("l.next_action_at < now()"));
    }

    @Test
    void firstContactAlertCountsUnansweredNewLeadsNotHistoricLateTransitions() {
        var jdbc = mock(NamedParameterJdbcTemplate.class);
        var service = new AnalyticsService(jdbc);
        var branch = UUID.randomUUID();
        when(jdbc.queryForObject(contains("l.last_contact_at is null"), any(SqlParameterSource.class), eq(Long.class))).thenReturn(2L);
        assertEquals(2, service.getDashboardLeadAnalytics(branch, LocalDate.of(2026, 9, 14), "Asia/Almaty").slaBreachedLeads());
        verify(jdbc).queryForObject(argThat(sql -> sql.contains("l.status = 'NEW'") &&
                sql.contains("l.created_at < now() - interval '2 hours'") &&
                !sql.contains("join lead_activities")), argThat((SqlParameterSource p) -> branch.equals(p.getValue("branchId"))), eq(Long.class));
    }
}
