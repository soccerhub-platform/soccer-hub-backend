package kz.edu.soccerhub.admin.application.service;

import kz.edu.soccerhub.client.domain.enums.ContractStatus;
import kz.edu.soccerhub.common.dto.admin.AdminDto;
import kz.edu.soccerhub.common.dto.contract.ContractDetailsOutput;
import kz.edu.soccerhub.common.dto.contract.ContractCapabilitiesOutput;
import kz.edu.soccerhub.common.dto.contract.ContractGroupOutput;
import kz.edu.soccerhub.common.dto.contract.ContractListItemOutput;
import kz.edu.soccerhub.common.dto.contract.ContractParticipantOutput;
import kz.edu.soccerhub.common.dto.contract.ContractPrimaryContactOutput;
import kz.edu.soccerhub.common.dto.contract.ContractSearchQuery;
import kz.edu.soccerhub.common.dto.contract.ContractsPageOutput;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentStatus;
import kz.edu.soccerhub.common.dto.payment.ContractPaymentSummaryOutput;
import kz.edu.soccerhub.common.port.ContractPort;
import kz.edu.soccerhub.common.port.PaymentPort;
import kz.edu.soccerhub.crm.domain.model.enums.LeadType;
import kz.edu.soccerhub.organization.domain.model.enums.GroupAudienceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminContractServicePaymentReadModelTest {

    @Mock
    private ContractPort contractPort;
    @Mock
    private PaymentPort paymentPort;
    @Mock
    private AdminService adminService;
    @Mock
    private AdminBranchService adminBranchService;

    private AdminContractService service;

    @BeforeEach
    void setUp() {
        service = new AdminContractService(contractPort, paymentPort, adminService, adminBranchService);
    }

    @Test
    void getContractShouldEnrichDetailWithPaymentSummary() {
        UUID adminId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(contractPort.getBranchId(contractId)).thenReturn(branchId);
        when(contractPort.getById(contractId)).thenReturn(contractDetails(contractId, branchId, BigDecimal.valueOf(80000)));
        when(paymentPort.getContractPaymentSummaries(any())).thenReturn(Map.of(
                contractId,
                new ContractPaymentSummaryOutput(
                        contractId,
                        BigDecimal.valueOf(80000),
                        BigDecimal.valueOf(50000),
                        BigDecimal.valueOf(30000),
                        BigDecimal.ZERO,
                        ContractPaymentStatus.PARTIALLY_PAID,
                        LocalDateTime.of(2026, 6, 23, 12, 0),
                        1
                )
        ));

        ContractDetailsOutput output = service.getContract(adminId, contractId);

        assertEquals(ContractPaymentStatus.PARTIALLY_PAID, output.paymentStatus());
        assertEquals(BigDecimal.valueOf(50000), output.paidAmount());
        assertEquals(BigDecimal.valueOf(30000), output.outstandingAmount());
        assertEquals(BigDecimal.ZERO, output.overpaidAmount());
        assertEquals(LocalDateTime.of(2026, 6, 23, 12, 0), output.lastPaidAt());
    }

    @Test
    void getContractsShouldBatchEnrichListWithoutNPlusOneSummaryCalls() {
        UUID adminId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID firstContractId = UUID.randomUUID();
        UUID secondContractId = UUID.randomUUID();

        when(adminService.findById(adminId)).thenReturn(Optional.of(admin(adminId)));
        when(adminBranchService.verifyAdminBelongsToBranch(adminId, branchId)).thenReturn(true);
        when(contractPort.search(any(), eq(PageRequest.of(0, 20)))).thenReturn(new ContractsPageOutput(
                List.of(
                        contractListItem(firstContractId, branchId, BigDecimal.valueOf(80000)),
                        contractListItem(secondContractId, branchId, BigDecimal.valueOf(60000))
                ),
                2,
                1,
                0,
                20
        ));
        when(paymentPort.getContractPaymentSummaries(any())).thenReturn(Map.of(
                firstContractId,
                new ContractPaymentSummaryOutput(
                        firstContractId,
                        BigDecimal.valueOf(80000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(80000),
                        BigDecimal.ZERO,
                        ContractPaymentStatus.UNPAID,
                        null,
                        0
                ),
                secondContractId,
                new ContractPaymentSummaryOutput(
                        secondContractId,
                        BigDecimal.valueOf(60000),
                        BigDecimal.valueOf(70000),
                        BigDecimal.ZERO,
                        BigDecimal.valueOf(10000),
                        ContractPaymentStatus.PAID,
                        LocalDateTime.of(2026, 6, 24, 12, 0),
                        2
                )
        ));

        ContractsPageOutput output = service.getContracts(
                adminId,
                new ContractSearchQuery(branchId, null, Set.of(), null, null),
                PageRequest.of(0, 20)
        );

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
        verify(paymentPort).getContractPaymentSummaries(captor.capture());

        assertEquals(2, captor.getValue().size());
        assertEquals(ContractPaymentStatus.UNPAID, output.content().get(0).paymentStatus());
        assertEquals(BigDecimal.ZERO, output.content().get(0).paidAmount());
        assertNull(output.content().get(0).lastPaidAt());
        assertEquals(ContractPaymentStatus.PAID, output.content().get(1).paymentStatus());
        assertEquals(BigDecimal.valueOf(10000), output.content().get(1).overpaidAmount());
    }

    private AdminDto admin(UUID adminId) {
        return AdminDto.builder()
                .id(adminId)
                .firstName("Admin")
                .lastName("User")
                .branchesId(Set.of())
                .build();
    }

    private ContractDetailsOutput contractDetails(UUID contractId, UUID branchId, BigDecimal amount) {
        return new ContractDetailsOutput(
                contractId,
                "CNT-2026-00001",
                branchId,
                LeadType.CHILDREN,
                ContractStatus.ACTIVE,
                amount,
                "KZT",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "note",
                new ContractParticipantOutput(UUID.randomUUID(), "Alex Doe", LocalDate.of(2015, 5, 10)),
                new ContractPrimaryContactOutput(UUID.randomUUID(), "Jane Doe", "+77010000000", "jane@example.com"),
                new ContractGroupOutput(UUID.randomUUID(), "Group A", GroupAudienceType.CHILDREN),
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 2, 10, 0),
                List.of(),
                new ContractCapabilitiesOutput(true, true, false, true, false, false)
        );
    }

    private ContractListItemOutput contractListItem(UUID contractId, UUID branchId, BigDecimal amount) {
        return new ContractListItemOutput(
                contractId,
                "CNT-2026-00001",
                branchId,
                LeadType.CHILDREN,
                ContractStatus.ACTIVE,
                amount,
                "KZT",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                "note",
                new ContractParticipantOutput(UUID.randomUUID(), "Alex Doe", LocalDate.of(2015, 5, 10)),
                new ContractPrimaryContactOutput(UUID.randomUUID(), "Jane Doe", "+77010000000", "jane@example.com"),
                new ContractGroupOutput(UUID.randomUUID(), "Group A", GroupAudienceType.CHILDREN),
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDateTime.of(2026, 6, 1, 10, 0),
                LocalDateTime.of(2026, 6, 2, 10, 0)
        );
    }
}
