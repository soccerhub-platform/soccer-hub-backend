package kz.edu.soccerhub.payments.domain.repository;

import kz.edu.soccerhub.payments.domain.enums.PaymentMethod;
import kz.edu.soccerhub.payments.domain.enums.PaymentStatus;
import kz.edu.soccerhub.payments.domain.model.Payment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
class PaymentRepositoryTest {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void searchShouldHandleNullableDateFilters() {
        UUID branchId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 23, 12, 0);

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("""
                insert into client_profiles (
                    id, first_name, last_name, phone, source, status, comments, branch_id,
                    created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                clientId,
                "Test",
                "Client",
                null,
                null,
                "ACTIVE",
                null,
                branchId,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.update("""
                insert into players (
                    id, first_name, last_name, birth_date, position, parent_id,
                    created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                playerId,
                "Test",
                "Player",
                java.sql.Date.valueOf("2016-05-10"),
                null,
                clientId,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.update("""
                insert into contracts (
                    id, player_id, client_id, group_id, contract_number, lead_type, status, coach_id,
                    start_date, end_date, amount, currency, notes, cancel_reason_code, cancel_comment,
                    created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                contractId,
                playerId,
                clientId,
                groupId,
                "CTR-TEST-001",
                "CHILDREN",
                "ACTIVE",
                null,
                java.sql.Date.valueOf("2026-06-01"),
                java.sql.Date.valueOf("2026-06-30"),
                BigDecimal.valueOf(15000),
                "KZT",
                null,
                null,
                null,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.update("""
                insert into payments (
                    id, contract_id, client_id, player_id, branch_id, amount, currency,
                    status, method, paid_at, recorded_at, recorded_by, comment, external_reference,
                    cancel_reason, cancel_comment, created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                contractId,
                clientId,
                playerId,
                branchId,
                BigDecimal.valueOf(15000),
                "KZT",
                PaymentStatus.PAID.name(),
                PaymentMethod.KASPI.name(),
                timestamp,
                timestamp,
                recordedBy,
                null,
                null,
                null,
                null,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        Page<Payment> page = paymentRepository.search(
                branchId,
                null,
                null,
                null,
                null,
                List.of(),
                true,
                List.of(),
                true,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertEquals(1, page.getTotalElements());
        assertEquals(contractId, page.getContent().get(0).getContractId());
    }

    @Test
    void searchShouldMatchRelatedFieldsAndFreeText() {
        UUID branchId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        UUID recordedBy = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 6, 23, 12, 0);

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        jdbcTemplate.update("""
                insert into client_profiles (
                    id, first_name, last_name, phone, source, status, comments, branch_id,
                    created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                clientId,
                "Ivan",
                "Petrov",
                "+77001234567",
                null,
                "ACTIVE",
                null,
                branchId,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.update("""
                insert into players (
                    id, first_name, last_name, birth_date, position, parent_id,
                    created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                playerId,
                "Artem",
                "Sokolov",
                java.sql.Date.valueOf("2016-05-10"),
                null,
                clientId,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.update("""
                insert into contracts (
                    id, player_id, client_id, group_id, contract_number, lead_type, status, coach_id,
                    start_date, end_date, amount, currency, notes, cancel_reason_code, cancel_comment,
                    created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                contractId,
                playerId,
                clientId,
                groupId,
                "CTR-2026-001",
                "CHILDREN",
                "ACTIVE",
                null,
                java.sql.Date.valueOf("2026-06-01"),
                java.sql.Date.valueOf("2026-06-30"),
                BigDecimal.valueOf(15000),
                "KZT",
                null,
                null,
                null,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.update("""
                insert into payments (
                    id, contract_id, client_id, player_id, branch_id, amount, currency,
                    status, method, paid_at, recorded_at, recorded_by, comment, external_reference,
                    cancel_reason, cancel_comment, created_at, updated_at, created_by, modified_by
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                paymentId,
                contractId,
                clientId,
                playerId,
                branchId,
                BigDecimal.valueOf(15000),
                "KZT",
                PaymentStatus.PAID.name(),
                PaymentMethod.KASPI.name(),
                timestamp,
                timestamp,
                recordedBy,
                "June payment",
                "KASPI-REF-001",
                null,
                null,
                timestamp,
                timestamp,
                null,
                null
        );
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        assertEquals(1, search(branchId, "ctr-2026-001").getTotalElements());
        assertEquals(1, search(branchId, "ivan petrov").getTotalElements());
        assertEquals(1, search(branchId, "artem sokolov").getTotalElements());
        assertEquals(1, search(branchId, "kaspi-ref-001").getTotalElements());
        assertEquals(1, search(branchId, "june payment").getTotalElements());

        Page<Payment> byUuid = paymentRepository.search(
                branchId,
                null,
                null,
                null,
                paymentId,
                List.of(),
                true,
                List.of(),
                true,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertEquals(1, byUuid.getTotalElements());
        assertTrue(byUuid.getContent().stream().anyMatch(payment -> payment.getId().equals(paymentId)));
    }

    private Page<Payment> search(UUID branchId, String text) {
        return paymentRepository.search(
                branchId,
                null,
                null,
                "%" + text.toLowerCase() + "%",
                null,
                List.of(),
                true,
                List.of(),
                true,
                null,
                null,
                PageRequest.of(0, 20)
        );
    }
}
