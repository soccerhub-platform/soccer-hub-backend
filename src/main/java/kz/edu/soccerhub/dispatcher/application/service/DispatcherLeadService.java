package kz.edu.soccerhub.dispatcher.application.service;

import kz.edu.soccerhub.common.dto.lead.LeadCreateCommand;
import kz.edu.soccerhub.common.dto.lead.LeadCreateOutput;
import kz.edu.soccerhub.common.dto.lead.LeadPrimaryContactInput;
import kz.edu.soccerhub.common.port.LeadPort;
import kz.edu.soccerhub.dispatcher.application.dto.DispatcherLeadCreateInput;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DispatcherLeadService {

    private final LeadPort leadPort;

    @Transactional
    public LeadCreateOutput createLead(DispatcherLeadCreateInput input) {
        LeadCreateCommand command = toCommand(input);
        List<UUID> leadIds = leadPort.createLeads(command);
        return new LeadCreateOutput(leadIds);
    }

    private LeadCreateCommand toCommand(DispatcherLeadCreateInput input) {
        return new LeadCreateCommand(
                input.leadType(),
                new LeadPrimaryContactInput(
                        trim(input.primaryContact().fullName()),
                        normalizePhone(input.primaryContact().phone()),
                        trim(input.primaryContact().email())
                ),
                trim(input.comment()),
                null,
                input.branchId(),
                input.participants()
        );
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replace(" ", "").trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }
}
