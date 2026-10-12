package kz.edu.soccerhub.trial.application.service;

import kz.edu.soccerhub.common.dto.company.TimelineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TrialOperationJournal {
    private final NamedParameterJdbcTemplate jdbc;

    public void append(UUID trial, UUID actor, String type, String description) {
        jdbc.update("insert into trial_operation_history(id,trial_id,actor_id,occurred_at,type,description) values(:id,:trial,:actor,:now,:type,:description)",
                new MapSqlParameterSource("id", UUID.randomUUID()).addValue("trial", trial)
                        .addValue("actor", actor).addValue("now", Timestamp.from(Instant.now()))
                        .addValue("type", type).addValue("description", description));
    }

    public List<TimelineEvent> history(UUID id) {
        return jdbc.query("select * from trial_operation_history where trial_id=:id order by occurred_at desc,id limit 100",
                new MapSqlParameterSource("id", id), (r,n) -> new TimelineEvent(r.getObject("id",UUID.class),
                        r.getString("type"),r.getString("description"),r.getTimestamp("occurred_at").toInstant(),r.getObject("actor_id",UUID.class)));
    }
}
