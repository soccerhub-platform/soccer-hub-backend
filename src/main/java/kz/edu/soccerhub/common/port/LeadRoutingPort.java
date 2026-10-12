package kz.edu.soccerhub.common.port;
import kz.edu.soccerhub.common.dto.lead.LeadRoutingCommand;
import java.util.UUID;
public interface LeadRoutingPort { void route(UUID id, UUID actor, LeadRoutingCommand command); }
