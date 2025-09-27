package com.chronos.service.executor;

import com.chronos.api.dto.job.payload.ReportJobPayload;
import com.chronos.domain.model.Job;
import com.chronos.domain.model.JobRun;
import com.chronos.exception.JobExecutionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import com.chronos.service.JobExecutorService;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReportJobExecutor {
    
    private final ApplicationContext applicationContext;
    private final ObjectMapper objectMapper;
    private final SpringTemplateEngine templateEngine;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void execute(Job job, JobRun run) {
        try {
            ReportJobPayload payload = objectMapper.convertValue(
                job.getPayload(), ReportJobPayload.class);

            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Generating %s report using template %s", 
                    payload.getReportType(), payload.getTemplateName()));

            byte[] reportContent = generateReport(payload, run);
            String outputPath = determineOutputPath(payload);
            
            saveReport(reportContent, outputPath, payload.getCompress());
            
            if (payload.getRecipients() != null && !payload.getRecipients().isEmpty()) {
                sendReportToRecipients(reportContent, payload, run);
            }

            getJobExecutorService().logOutput(run, "INFO", 
                String.format("Report generated successfully at %s", outputPath));

        } catch (Exception e) {
            String error = String.format("Report generation failed: %s", e.getMessage());
            getJobExecutorService().logOutput(run, "ERROR", error);
            throw new JobExecutionException(error, e);
        }
    }

    @Retryable(maxAttempts = 3, backoff = @Backoff(delay = 1000))
    private byte[] generateReport(ReportJobPayload payload, JobRun run) {
        try {
            Map<String, Object> data = fetchReportData(payload);
            Resource template = new ClassPathResource(
                "reports/" + payload.getTemplateName());

            Context context = new Context();
            context.setVariables(data);
            if (payload.getTimezone() != null) {
                context.setLocale(Locale.forLanguageTag(payload.getTimezone()));
            }

            String content = templateEngine.process(template.getFilename(), context);

            switch (payload.getReportType().toUpperCase()) {
                case "PDF":
                    return generatePdfReport(content, payload.getFormatting());
                case "EXCEL":
                    return generateExcelReport(data, payload.getFormatting());
                case "CSV":
                    return generateCsvReport(data);
                case "HTML":
                    return content.getBytes();
                default:
                    throw new JobExecutionException("Unsupported report type: " + 
                        payload.getReportType());
            }
        } catch (Exception e) {
            throw new JobExecutionException("Failed to generate report", e);
        }
    }

    private Map<String, Object> fetchReportData(ReportJobPayload payload) {
        // In a real implementation, this would execute queries or fetch data
        // from various sources based on the report parameters
        Map<String, Object> data = new HashMap<>(payload.getParameters());
        
        // Add standard report metadata
        data.put("generatedAt", Instant.now());
        data.put("dateRange", payload.getDateRange());
        
        return data;
    }

    private byte[] generatePdfReport(String htmlContent, Map<String, Object> formatting) {
        // In a real implementation, this would use a PDF library like Flying Saucer
        // or iText to convert HTML to PDF with the specified formatting
        return htmlContent.getBytes();
    }

    private byte[] generateExcelReport(Map<String, Object> data, Map<String, Object> formatting) {
        // In a real implementation, this would use Apache POI to generate Excel files
        // with proper formatting, formulas, etc.
        return new byte[0];
    }

    private byte[] generateCsvReport(Map<String, Object> data) {
        // In a real implementation, this would use OpenCSV or Apache Commons CSV
        // to generate CSV files from the data
        return new byte[0];
    }

    private String determineOutputPath(ReportJobPayload payload) {
        if (payload.getOutputPath() != null) {
            return payload.getOutputPath();
        }

        String timestamp = DateTimeFormatter
            .ofPattern("yyyyMMdd_HHmmss")
            .withZone(ZoneId.of(payload.getTimezone()))
            .format(Instant.now());

        return String.format("reports/%s_%s.%s",
            payload.getTemplateName().replaceAll("[^a-zA-Z0-9]", "_"),
            timestamp,
            payload.getReportType().toLowerCase());
    }

    private void saveReport(byte[] content, String outputPath, Boolean compress) throws Exception {
        Path path = Path.of(outputPath);
        Files.createDirectories(path.getParent());

        if (Boolean.TRUE.equals(compress)) {
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(outputPath + ".zip"))) {
                ZipEntry entry = new ZipEntry(path.getFileName().toString());
                zos.putNextEntry(entry);
                zos.write(content);
                zos.closeEntry();
            }
        } else {
            Files.write(path, content);
        }
    }

    private void sendReportToRecipients(byte[] content, ReportJobPayload payload, JobRun run) {
        // In a real implementation, this would use NotificationService to send
        // the report via email to the specified recipients
            applicationContext.getBean(JobExecutorService.class).logOutput(run, "INFO", 
                String.format("Sending report to %d recipients", payload.getRecipients().size()));
    }

    private JobExecutorService getJobExecutorService() {
        return applicationContext.getBean(JobExecutorService.class);
    }
}
