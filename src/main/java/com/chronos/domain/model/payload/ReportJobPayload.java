package com.chronos.domain.model.payload;

import com.chronos.domain.model.JobPayload;
import com.chronos.domain.model.enums.JobType;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@DiscriminatorValue("REPORT")
@JsonTypeName("REPORT")
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class ReportJobPayload extends JobPayload {
    private static final List<String> VALID_FORMATS = List.of(
        "PDF", "EXCEL", "CSV", "HTML", "JSON"
    );

    @NotBlank(message = "Report type is required")
    @Column(name = "report_type")
    private String reportType;

    @Column(name = "parameters", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> parameters;

    @Column(name = "recipients", columnDefinition = "json")
    @Type(JsonType.class)
    private List<String> recipients;

    @Column(name = "format")
    private String format;

    @Column(name = "compress")
    private Boolean compress;

    @Column(name = "template_name")
    private String templateName;

    @Column(name = "template_data", columnDefinition = "json")
    @Type(JsonType.class)
    private Map<String, Object> templateData;

    @Override
    public void validate(JobType type) {
        if (type != JobType.REPORT) {
            throw new IllegalArgumentException("Invalid job type for ReportJobPayload: " + type);
        }

        if (reportType == null || reportType.isBlank()) {
            throw new IllegalArgumentException("Report type is required for REPORT jobs");
        }

        if (parameters == null) {
            throw new IllegalArgumentException("Report parameters must not be null");
        }

        if (format != null && !VALID_FORMATS.contains(format.toUpperCase())) {
            throw new IllegalArgumentException("Invalid report format: " + format + 
                ". Valid formats are: " + String.join(", ", VALID_FORMATS));
        }

        if (templateName != null && templateData == null) {
            throw new IllegalArgumentException("Template data is required when template name is specified");
        }

        if (recipients != null) {
            for (String recipient : recipients) {
                if (recipient == null || recipient.isBlank()) {
                    throw new IllegalArgumentException("Recipients cannot contain empty values");
                }
                if (!recipient.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    throw new IllegalArgumentException("Invalid email format for recipient: " + recipient);
                }
            }
        }
    }
}
