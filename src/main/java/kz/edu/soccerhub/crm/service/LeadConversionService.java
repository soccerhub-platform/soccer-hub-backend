package kz.edu.soccerhub.crm.service;

import java.util.UUID;

public interface LeadConversionService {

    UUID convertLeadToClient(UUID leadId);
}

