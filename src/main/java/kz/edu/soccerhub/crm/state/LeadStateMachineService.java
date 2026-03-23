package kz.edu.soccerhub.crm.state;

import kz.edu.soccerhub.common.exception.BadRequestException;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.service.StateMachineService;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LeadStateMachineService {

    private final StateMachineService<LeadStatus, LeadEvent> service;

    public LeadStatus process(UUID leadId,
                              LeadStatus currentState,
                              LeadEvent event) {

        StateMachine<LeadStatus, LeadEvent> sm =
                service.acquireStateMachine(leadId.toString());

        sm.stopReactively().block();

        sm.getStateMachineAccessor()
                .doWithAllRegions(access ->
                        access.resetStateMachineReactively(
                                new DefaultStateMachineContext<>(
                                        currentState, null, null, null
                                )
                        ).block()
                );

        sm.startReactively().block();

        StateMachineEventResult<LeadStatus, LeadEvent> result = sm.sendEvent(
                Mono.just(MessageBuilder.withPayload(event).build())
        ).blockFirst();

        if (result == null || result.getResultType() == StateMachineEventResult.ResultType.DENIED) {
            // Обязательно освобождаем машину перед исключением
            service.releaseStateMachine(leadId.toString());
            throw new BadRequestException("Transition not allowed: " + currentState + " -> " + event);
        }

        LeadStatus newState = sm.getState().getId();

        // 5. Освобождение ресурсов
        service.releaseStateMachine(leadId.toString());

        return newState;
    }
}
