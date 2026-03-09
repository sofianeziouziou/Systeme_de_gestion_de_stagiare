package com.hikma.stagiaires.dto.projet;

import com.hikma.stagiaires.model.ProjetStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class ProjetDTOs {

    @Data
    public static class CreateRequest {
        @NotBlank @Size(max = 100)
        private String title;

        private String description;

        @NotEmpty
        private List<String> stagiaireIds;

        @NotNull
        private LocalDate startDate;

        @NotNull
        private LocalDate plannedEndDate;

        private List<String> technologies;
        private List<JalonRequest> jalons;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 100)
        private String title;

        private String description;
        private List<String> stagiaireIds;
        private LocalDate startDate;
        private LocalDate plannedEndDate;
        private LocalDate actualEndDate;
        private Integer progress;
        private ProjetStatus status;
        private List<String> technologies;
        private List<JalonRequest> jalons;
    }

    @Data
    public static class JalonRequest {
        @NotBlank
        private String title;

        @NotNull
        private LocalDate date;

        private boolean completed;
    }

    @Data
    public static class ProjetResponse {
        private String id;
        private String title;
        private String description;
        private List<String> stagiaireIds;
        private List<StagiaireInfo> stagiaires;
        private String tuteurId;
        private String tuteurName;
        private LocalDate startDate;
        private LocalDate plannedEndDate;
        private LocalDate actualEndDate;
        private Integer progress;
        private ProjetStatus status;
        private List<String> technologies;
        private List<JalonResponse> jalons;
        private String reportUrl;
        private LocalDate reportSubmittedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        @Data
        public static class StagiaireInfo {
            private String id;
            private String firstName;
            private String lastName;
            private String photoUrl;
        }

        @Data
        public static class JalonResponse {
            private String title;
            private LocalDate date;
            private boolean completed;
        }
    }

    @Data
    public static class ProjetSummary {
        private String id;
        private String title;
        private Integer progress;
        private ProjetStatus status;
        private LocalDate plannedEndDate;
        private int stagiaireCount;
    }
}