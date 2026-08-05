package kz.edu.soccerhub.crm.application.state;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringJUnitConfig
@ContextConfiguration(classes = LeadStateMachineConfig.class)
class LeadStateMachineServiceTest {

    @Autowired
    private StateMachineService<LeadStatus, LeadEvent> stateMachineService;

    private LeadStateMachineService service;

    @BeforeEach
    void setUp() {
        service = new LeadStateMachineService(stateMachineService);
    }

    @Test
    void startsContractWithoutTrial() {
        LeadStatus result = service.process(
                UUID.randomUUID(),
                LeadStatus.IN_PROGRESS,
                LeadEvent.START_CONTRACT
        );

        assertEquals(LeadStatus.CONTRACT_PENDING, result);
    }

    @Test
    void startsContractAfterTrial() {
        LeadStatus result = service.process(
                UUID.randomUUID(),
                LeadStatus.DECISION_PENDING,
                LeadEvent.START_CONTRACT
        );

        assertEquals(LeadStatus.CONTRACT_PENDING, result);
    }

    @Test
    void activeContractMovesLeadToPaymentPending() {
        LeadStatus result = service.process(
                UUID.randomUUID(),
                LeadStatus.CONTRACT_PENDING,
                LeadEvent.CONTRACT_ACTIVATED
        );

        assertEquals(LeadStatus.PAYMENT_PENDING, result);
    }

    @Test
    void firstPaymentCompletesLead() {
        LeadStatus result = service.process(
                UUID.randomUUID(),
                LeadStatus.PAYMENT_PENDING,
                LeadEvent.FIRST_PAYMENT_RECEIVED
        );

        assertEquals(LeadStatus.CONVERTED, result);
    }

    @Test
    void cannotStartContractFromNewLead() {
        assertThrows(
                BadRequestException.class,
                () -> service.process(
                        UUID.randomUUID(),
                        LeadStatus.NEW,
                        LeadEvent.START_CONTRACT
                )
        );
    }
}