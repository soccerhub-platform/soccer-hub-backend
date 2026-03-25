package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherLeadCreateInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatcherLeadService {

    private final LeadPort leadPort;

    @Transactional
    public LeadCreateOutput createLead(DispatcherLeadCreateInput input) {
        LeadCreateCommand command = toCommand(input);
        UUID leadId = leadPort.createLead(command);
        return new LeadCreateOutput(leadId);
    }

    private LeadCreateCommand toCommand(DispatcherLeadCreateInput input) {
        return new LeadCreateCommand(
                trim(input.parentName()),
                normalizePhone(input.phone()),
                trim(input.email()),
                trim(input.comment()),
                null,
                input.branchId(),
                input.children()
        );
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replace(" ", "").trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}

