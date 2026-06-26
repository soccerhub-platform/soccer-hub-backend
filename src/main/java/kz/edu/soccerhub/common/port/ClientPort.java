package kz.edu.soccerhub.common.port;

import kz.edu.soccerhub.common.dto.client.ClientCreateCommand;
import kz.edu.soccerhub.common.dto.client.ClientCreateCommandOutput;
import kz.edu.soccerhub.common.dto.client.ClientConversionCommand;
import kz.edu.soccerhub.common.dto.client.ClientConversionOutput;
import kz.edu.soccerhub.common.dto.client.GroupMemberDto;
import kz.edu.soccerhub.common.dto.student.StudentProfileDto;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

public interface ClientPort {

    UUID createClient(
            String parentName,
            String phone,
            String email
    );

    UUID createPlayer(
            UUID clientId,
            String childName,
            Integer childAge
    );

    ClientCreateCommandOutput create(ClientCreateCommand command);

    List<GroupMemberDto> getGroupMembers(UUID groupId);

    ClientConversionOutput convertLead(ClientConversionCommand command);

    List<StudentProfileDto> getStudentProfilesByBranch(UUID branchId);

    StudentProfileDto getStudentProfile(UUID playerId);

    long countStudentsAsOf(
            UUID branchId,
            LocalDate date,
            String timezone
    );

    long countCreatedStudents(
            UUID branchId,
            LocalDate date,
            String timezone
    );
}
