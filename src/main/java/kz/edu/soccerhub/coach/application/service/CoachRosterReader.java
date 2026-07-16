package kz.edu.soccerhub.coach.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CoachRosterReader {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<ActivePlayerView> getActivePlayersByGroupAndDate(UUID groupId, LocalDate sessionDate) {
        String sql = """
                select distinct p.id, p.first_name, p.last_name
                from players p
                join group_memberships gm on gm.player_id = p.id
                where gm.group_id = :groupId
                  and gm.joined_at <= :sessionDate
                  and (gm.left_at is null or gm.left_at >= :sessionDate)
                  and (
                      :sessionDate < :today
                      or gm.status in ('ACTIVE', 'UPCOMING')
                  )
                order by p.first_name, p.last_name
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("sessionDate", sessionDate)
                .addValue("today", LocalDate.now());

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ActivePlayerView(
                UUID.fromString(rs.getString("id")),
                rs.getString("first_name"),
                rs.getString("last_name")
        ));
    }

    public record ActivePlayerView(
            UUID id,
            String firstName,
            String lastName
    ) {
    }
}
