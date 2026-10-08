package kz.edu.soccerhub.crm.application.service;
import kz.edu.soccerhub.common.dto.lead.LeadWorkCommand;
import kz.edu.soccerhub.common.dto.lead.LeadPreferencesCommand;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import kz.edu.soccerhub.common.exception.*;
import kz.edu.soccerhub.common.port.LeadWorkPort;
import kz.edu.soccerhub.crm.domain.model.Lead;
import kz.edu.soccerhub.crm.domain.model.enums.LeadStatus;
import kz.edu.soccerhub.crm.domain.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
@Service
@RequiredArgsConstructor
public class LeadWorkService implements LeadWorkPort {
    private final LeadRepository repository;
    private final LeadActivityService activities;
    private final LeadService leads;
    private final ObjectMapper objectMapper;

    @Override @Transactional
    public void updatePreferences(UUID leadId, UUID actorId, LeadPreferencesCommand c) {
        Lead lead = repository.findLockedById(leadId)
                .orElseThrow(() -> new NotFoundException("Лид не найден", leadId));
        if (c.version() == null || c.version() != lead.getWorkVersion())
            throw new ConflictException("Лид уже изменён. Обновите карточку и повторите действие.",
                    "LEAD_WORK_CONFLICT", Map.of("leadId", leadId));
        if (lead.getStatus() == LeadStatus.LOST || lead.getStatus() == LeadStatus.CONVERTED)
            throw new BadRequestException("Пожелания закрытого лида доступны только для просмотра");
        var days = java.util.List.of("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN");
        if (c.preferredDays() == null || c.preferredDays().size() > 7
                || c.preferredDays().stream().anyMatch(d -> d == null || !days.contains(d)))
            throw new BadRequestException("Некорректные дни недели");
        String experience = c.experience() == null ? "" : c.experience().trim();
        String notes = c.notes() == null ? "" : c.notes().trim();
        if (!java.util.List.of("", "BEGINNER", "INTERMEDIATE", "ADVANCED").contains(experience) || notes.length() > 1000)
            throw new BadRequestException("Проверьте уровень подготовки и длину заметки");
        String preferredDays = String.join(",", days.stream().filter(c.preferredDays()::contains).toList());
        // Keep the legacy read-model in sync, without touching participant identities or trial bookings.
        ObjectNode data = objectMapper.createObjectNode();
        try {
            if (lead.getQualificationData() != null && objectMapper.readTree(lead.getQualificationData()) instanceof ObjectNode old)
                data = old;
        } catch (com.fasterxml.jackson.core.JsonProcessingException ignored) { /* Legacy non-JSON text. */ }
        data.put("preferredDays", preferredDays);
        data.put("timePreference", c.timePreference() == null ? null : c.timePreference().name());
        data.put("experience", experience);
        data.put("notes", notes);
        lead.updateQualificationData(data.toString());
        lead.updateQualificationFields(preferredDays, experience, c.timePreference(), notes);
        lead.updateWork(lead.getWorkPriority(), lead.getNextAction(), lead.getNextActionAt(), false);
        activities.logWork(lead, actorId, "Пожелания к занятиям обновлены · дни: "
                + (preferredDays.isEmpty() ? "не уточнены" : preferredDays)
                + " · время: " + (c.timePreference() == null ? "не уточнено" : c.timePreference()));
    }

    @Override @Transactional
    public void update(UUID leadId, UUID actorId, LeadWorkCommand c) {
        Lead lead = repository.findLockedById(leadId)
                .orElseThrow(() -> new NotFoundException("Лид не найден", leadId));
        if (c.version() == null || c.version() != lead.getWorkVersion()) {
            throw new ConflictException("Лид уже изменён. Обновите карточку и повторите действие.",
                    "LEAD_WORK_CONFLICT", Map.of("leadId", leadId));
        }
        if (lead.getStatus() == LeadStatus.LOST || lead.getStatus() == LeadStatus.CONVERTED) {
            throw new BadRequestException("Рабочие действия недоступны для закрытого лида");
        }
        if (c.operation() == null || c.priority() == null) throw new BadRequestException("Укажите действие и приоритет");
        String action = c.nextAction() == null ? "" : c.nextAction().trim();
        if (c.operation() != LeadWorkCommand.Operation.COMPLETE && (action.isEmpty() != (c.nextActionAt() == null))) {
            throw new BadRequestException("Укажите следующее действие и срок вместе");
        }
        String description;
        if (c.operation() == LeadWorkCommand.Operation.COMPLETE) {
            if (lead.getNextAction() == null) throw new BadRequestException("Нет действия для завершения");
            description = "Выполнено: " + lead.getNextAction();
            lead.updateWork(lead.getWorkPriority(), null, null, false);
        } else {
            if (c.operation() == LeadWorkCommand.Operation.CONTACT && (c.channel() == null || c.outcome() == null)) {
                throw new BadRequestException("Укажите канал и результат контакта");
            }
            if (c.nextActionAt() != null && !c.nextActionAt().isAfter(Instant.now())
                    && !c.nextActionAt().equals(lead.getNextActionAt())) {
                throw new BadRequestException("Новый срок должен быть в будущем");
            }
            boolean contact = c.operation() == LeadWorkCommand.Operation.CONTACT;
            lead.updateWork(c.priority().name(), action.isEmpty() ? null : action, c.nextActionAt(), contact);
            description = contact
                    ? "Контакт · " + channel(c.channel()) + " · " + outcome(c.outcome())
                    : "План обновлён · приоритет: " + priority(c.priority());
            if (!action.isEmpty()) description += " · " + action + " · срок: " + c.nextActionAt();
            else description += " · следующее действие не назначено";
        }
        if (c.comment() != null && !c.comment().isBlank()) description += "\n" + c.comment().trim();
        activities.logWork(lead, actorId, description);
        if (c.operation() == LeadWorkCommand.Operation.CONTACT && lead.getStatus() == LeadStatus.NEW
                && (c.outcome() == LeadWorkCommand.Outcome.REACHED || c.outcome() == LeadWorkCommand.Outcome.FOLLOW_UP)) {
            leads.processEvent(leadId, kz.edu.soccerhub.crm.application.state.LeadEvent.CONTACT, null, null, actorId);
        }
    }
    private String channel(LeadWorkCommand.Channel value) {
        return switch(value) { case PHONE -> "Телефон"; case WHATSAPP -> "WhatsApp"; case EMAIL -> "Email"; case IN_PERSON -> "Лично"; };
    }
    private String outcome(LeadWorkCommand.Outcome value) {
        return switch(value) { case REACHED -> "Связались"; case NO_ANSWER -> "Нет ответа"; case FOLLOW_UP -> "Договорились о повторном контакте"; case WRONG_NUMBER -> "Неверный номер"; };
    }
    private String priority(LeadWorkCommand.Priority value) {
        return switch(value) { case NORMAL -> "Обычный"; case HIGH -> "Высокий"; case URGENT -> "Срочный"; };
    }
}
