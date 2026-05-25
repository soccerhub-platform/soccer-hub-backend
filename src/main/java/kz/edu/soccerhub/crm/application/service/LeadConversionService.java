package kz.edu.soccerhub.crm.application.service;

import kz.edu.soccerhub.common.dto.lead.ConvertLeadRequest;
import kz.edu.soccerhub.common.dto.lead.ConvertLeadResponse;

import java.util.UUID;

public interface LeadConversionService {

    ConvertLeadResponse convertLeadToClient(UUID leadId, ConvertLeadRequest request, UUID currentAdminId);
}
