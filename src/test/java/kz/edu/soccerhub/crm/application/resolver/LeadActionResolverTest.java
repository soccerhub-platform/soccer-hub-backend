package kz.edu.soccerhub.crm.application.resolver;

import kz.edu.soccerhub.common.dto.lead.LeadActionOutput;
import kz.edu.soccerhub.common.dto.lead.LeadActionType;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadSource;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LeadActionResolverTest {

    private final LeadActionResolver resolver =
            new LeadActionResolver();

    @Test
    void inProgressOffersTrialAndConversionWithoutTrial() {
        Lead lead = lead(LeadStatus.IN_PROGRESS);
        lead.addParticipant("Алихан", null, null, null);

        var types = resolver.resolve(lead, UUID.randomUUID())
                .stream()
                .map(LeadActionOutput::type)
                .toList();

        assertTrue(types.contains(LeadActionType.SCHEDULE_TRIAL));
        assertTrue(types.contains(LeadActionType.CONVERT_TO_CLIENT));
    }

    @Test
    void contractPendingOffersContractCreation() {
        Lead lead = lead(LeadStatus.CONTRACT_PENDING);
        lead.linkClient(UUID.randomUUID());
        lead.addParticipant("Алихан", null, null, null);
        lead.getParticipants().getFirst()
                .linkPlayer(UUID.randomUUID());

        var action = resolver.resolve(lead, UUID.randomUUID())
                .stream()
                .filter(item ->
                        item.type() == LeadActionType.CREATE_CONTRACT
                )
                .findFirst()
                .orElseThrow();

        assertTrue(action.enabled());
    }

    @Test
    void paymentPendingOffersFirstPayment() {
        Lead lead = lead(LeadStatus.PAYMENT_PENDING);
        lead.linkClient(UUID.randomUUID());

        var types = resolver.resolve(lead, UUID.randomUUID())
                .stream()
                .map(LeadActionOutput::type)
                .toList();

        assertTrue(types.contains(LeadActionType.RECORD_PAYMENT));
    }

    private Lead lead(LeadStatus status) {
        return Lead.builder()
                .id(UUID.randomUUID())
                .leadType(LeadType.CHILDREN)
                .primaryContactName("Айгуль")
                .primaryContactPhone("+77001234567")
                .source(LeadSource.OTHER)
                .status(status)
                .branchId(UUID.randomUUID())
                .build();
    }
}