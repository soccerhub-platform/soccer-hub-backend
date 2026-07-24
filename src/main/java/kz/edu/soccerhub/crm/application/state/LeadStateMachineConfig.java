package kz.edu.soccerhub.crm.application.state;

import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.service.DefaultStateMachineService;
import org.springframework.statemachine.service.StateMachineService;

import java.util.Set;

@Configuration
@EnableStateMachineFactory
public class LeadStateMachineConfig extends StateMachineConfigurerAdapter<LeadStatus, LeadEvent> {

    @Override
    public void configure(StateMachineStateConfigurer<LeadStatus, LeadEvent> states)
            throws Exception {
        states.withStates()
                .initial(LeadStatus.NEW)
                .states(Set.of(LeadStatus.values()));
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<LeadStatus, LeadEvent> transitions)
            throws Exception {

        transitions
                .withExternal()
                    .source(LeadStatus.NEW)
                    .target(LeadStatus.IN_PROGRESS)
                    .event(LeadEvent.CONTACT)
                .and()
                .withExternal()
                    .source(LeadStatus.IN_PROGRESS)
                    .target(LeadStatus.IN_PROGRESS)
                    .event(LeadEvent.QUALIFY)
                .and()
                .withExternal()
                    .source(LeadStatus.IN_PROGRESS)
                    .target(LeadStatus.TRIAL_SCHEDULED)
                    .event(LeadEvent.SCHEDULE_TRIAL)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_SCHEDULED)
                    .target(LeadStatus.DECISION_PENDING)
                    .event(LeadEvent.COMPLETE_TRIAL)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_SCHEDULED)
                    .target(LeadStatus.IN_PROGRESS)
                    .event(LeadEvent.CANCEL_TRIAL)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_SCHEDULED)
                    .target(LeadStatus.IN_PROGRESS)
                    .event(LeadEvent.NO_SHOW)
                .and()
                .withExternal()
                    .source(LeadStatus.DECISION_PENDING)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.POST_TRIAL_REJECT)
                .and()
                .withExternal()
                    .source(LeadStatus.NEW)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.REJECT)
                .and()
                .withExternal()
                    .source(LeadStatus.IN_PROGRESS)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.REJECT)
                .and()
                .withExternal()
                    .source(LeadStatus.DECISION_PENDING)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.REJECT)
                ;
    }

    @Bean
    public StateMachineService<LeadStatus, LeadEvent> stateMachineService(
            StateMachineFactory<LeadStatus, LeadEvent> factory) {
        return new DefaultStateMachineService<>(factory);
    }
}
