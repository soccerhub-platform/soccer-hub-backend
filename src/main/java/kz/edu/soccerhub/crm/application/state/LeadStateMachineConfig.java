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
                    .target(LeadStatus.CONTACTED)
                    .event(LeadEvent.CONTACT)
                .and()
                .withExternal()
                    .source(LeadStatus.CONTACTED)
                    .target(LeadStatus.QUALIFIED)
                    .event(LeadEvent.QUALIFY)
                .and()
                .withExternal()
                    .source(LeadStatus.QUALIFIED)
                    .target(LeadStatus.TRIAL_SCHEDULED)
                    .event(LeadEvent.SCHEDULE_TRIAL)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_SCHEDULED)
                    .target(LeadStatus.TRIAL_DONE)
                    .event(LeadEvent.COMPLETE_TRIAL)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_SCHEDULED)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.NO_SHOW)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_DONE)
                    .target(LeadStatus.WAITING_PAYMENT)
                    .event(LeadEvent.REQUEST_PAYMENT)
                .and()
                .withExternal()
                    .source(LeadStatus.TRIAL_DONE)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.POST_TRIAL_REJECT)
                .and()
                .withExternal()
                    .source(LeadStatus.WAITING_PAYMENT)
                    .target(LeadStatus.WON)
                    .event(LeadEvent.CONFIRM_PAYMENT)
                .and()
                .withExternal()
                    .source(LeadStatus.NEW)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.REJECT)
                .and()
                .withExternal()
                    .source(LeadStatus.CONTACTED)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.REJECT)
                .and()
                .withExternal()
                    .source(LeadStatus.QUALIFIED)
                    .target(LeadStatus.LOST)
                    .event(LeadEvent.REJECT);
    }

    @Bean
    public StateMachineService<LeadStatus, LeadEvent> stateMachineService(
            StateMachineFactory<LeadStatus, LeadEvent> factory) {
        return new DefaultStateMachineService<>(factory);
    }
}
