package kz.edu.soccerhub.admin.application.dto.coach;

public final class AdminTrainerFilterEnums {
    private AdminTrainerFilterEnums() {
    }

    public enum GroupFilter {
        WITHOUT_GROUP,
        ONE_GROUP,
        TWO_OR_THREE_GROUPS,
        FOUR_OR_MORE_GROUPS
    }

    public enum WorkloadStatus {
        LOW,
        MEDIUM,
        HIGH,
        FULL,
        OVERLOADED
    }

    public enum ReportStatus {
        NO_REPORTS,
        PENDING,
        OVERDUE,
        SUBMITTED
    }

    public enum SortField {
        NAME,
        GROUP_COUNT,
        TODAY_SESSION_COUNT,
        WORKLOAD,
        LAST_REPORT_AT
    }
}
