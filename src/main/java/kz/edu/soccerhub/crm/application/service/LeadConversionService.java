package kz.edu.soccerhub.crm.application.service;

import java.util.UUID;

public interface LeadConversionService {

    UUID convertLeadToClient(UUID leadId);
}

