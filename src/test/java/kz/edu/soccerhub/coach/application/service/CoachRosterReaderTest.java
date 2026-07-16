package kz.edu.soccerhub.coach.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoachRosterReaderTest {

    private CoachRosterReader reader;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:coach_roster_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                ""
        );
        jdbcTemplate = new JdbcTemplate(dataSource);
        reader = new CoachRosterReader(new NamedParameterJdbcTemplate(dataSource));

        jdbcTemplate.execute("""
                create table players (
                    id uuid primary key,
                    first_name varchar(100),
                    last_name varchar(100)
                )
                """);
        jdbcTemplate.execute("""
                create table group_memberships (
                    id uuid primary key,
                    group_id uuid not null,
                    player_id uuid not null,
                    status varchar(32) not null,
                    joined_at date not null,
                    left_at date
                )
                """);
    }

    @Test
    void shouldExcludeClosedMembershipsFromCurrentAndFutureRosters() {
        UUID groupId = UUID.randomUUID();
        UUID activePlayerId = insertPlayer("Active", "Player");
        UUID removedPlayerId = insertPlayer("Removed", "Player");
        LocalDate today = LocalDate.now();

        insertMembership(groupId, activePlayerId, "ACTIVE", today.minusMonths(1), null);
        insertMembership(groupId, removedPlayerId, "REMOVED", today.minusMonths(1), today.plusMonths(1));

        List<CoachRosterReader.ActivePlayerView> roster = reader.getActivePlayersByGroupAndDate(
                groupId,
                today.plusDays(1)
        );

        assertEquals(List.of(activePlayerId), roster.stream().map(CoachRosterReader.ActivePlayerView::id).toList());
    }

    @Test
    void shouldKeepClosedMembershipsInHistoricalRosters() {
        UUID groupId = UUID.randomUUID();
        UUID removedPlayerId = insertPlayer("Removed", "Player");
        LocalDate today = LocalDate.now();

        insertMembership(groupId, removedPlayerId, "REMOVED", today.minusMonths(1), today.plusMonths(1));

        List<CoachRosterReader.ActivePlayerView> roster = reader.getActivePlayersByGroupAndDate(
                groupId,
                today.minusDays(1)
        );

        assertEquals(List.of(removedPlayerId), roster.stream().map(CoachRosterReader.ActivePlayerView::id).toList());
    }

    private UUID insertPlayer(String firstName, String lastName) {
        UUID playerId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into players (id, first_name, last_name) values (?, ?, ?)",
                playerId,
                firstName,
                lastName
        );
        return playerId;
    }

    private void insertMembership(
            UUID groupId,
            UUID playerId,
            String status,
            LocalDate joinedAt,
            LocalDate leftAt
    ) {
        jdbcTemplate.update(
                "insert into group_memberships (id, group_id, player_id, status, joined_at, left_at) values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(),
                groupId,
                playerId,
                status,
                joinedAt,
                leftAt
        );
    }
}
