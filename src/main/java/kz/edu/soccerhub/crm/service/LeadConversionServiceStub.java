package kz.edu.soccerhub.crm.service;

import kz.edu.soccerhub.common.exception.BadRequestException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class LeadConversionServiceStub implements LeadConversionService {

    @Override
    public UUID convertLeadToClient(UUID leadId) {
        throw new BadRequestException("Lead to client conversion is not implemented yet", leadId);
    }
}

