package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.lead.LeadWorkCommand;
import java.util.UUID;
public interface LeadWorkPort {
    void update(UUID leadId, UUID actorId, LeadWorkCommand command);
    void updatePreferences(UUID leadId, UUID actorId, kz.edu.soccerhub.common.dto.lead.LeadPreferencesCommand command);
}
