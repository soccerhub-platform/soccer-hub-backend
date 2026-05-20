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
                join contracts c on c.player_id = p.id
                where c.group_id = :groupId
                  and c.start_date <= :sessionDate
                  and (c.end_date is null or c.end_date >= :sessionDate)
                order by p.first_name, p.last_name
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("groupId", groupId)
                .addValue("sessionDate", sessionDate);

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
