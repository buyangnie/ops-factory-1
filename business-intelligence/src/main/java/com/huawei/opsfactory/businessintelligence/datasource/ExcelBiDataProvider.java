package com.huawei.opsfactory.businessintelligence.datasource;

import com.huawei.opsfactory.businessintelligence.config.BusinessIntelligenceRuntimeProperties;
import com.huawei.opsfactory.businessintelligence.model.BiColumns;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

@Component
public class ExcelBiDataProvider implements BiDataProvider {

    private static final Logger log = LoggerFactory.getLogger(ExcelBiDataProvider.class);

    private static final String INCIDENTS_FILE = "Incidents-exported.xlsx";
    private static final String CHANGES_FILE = "Changes-exported.xlsx";
    private static final String REQUESTS_FILE = "Requests-exported.xlsx";
    private static final String PROBLEMS_FILE = "Problems-exported.xlsx";
    private static final String SLA_FILE = "SLAs-exported.xlsx";

    private final BusinessIntelligenceRuntimeProperties runtimeProperties;
    private final DataFormatter formatter = new DataFormatter();

    public ExcelBiDataProvider(BusinessIntelligenceRuntimeProperties runtimeProperties) {
        this.runtimeProperties = runtimeProperties;
    }

    @Override
    public BiRawData load() {
        Path baseDir = Paths.get(runtimeProperties.getBaseDir()).toAbsolutePath().normalize();
        List<Map<String, String>> incidents = readRows(baseDir.resolve(INCIDENTS_FILE), "Data");
        List<Map<String, String>> incidentSlaCriteria = readRows(baseDir.resolve(SLA_FILE), "Incidents_SLA");
        List<Map<String, String>> requestSlaCriteria = readRows(baseDir.resolve(SLA_FILE), "Requests_SLA");
        List<Map<String, String>> changes = readRows(baseDir.resolve(CHANGES_FILE), "Data");
        List<Map<String, String>> requests = readRows(baseDir.resolve(REQUESTS_FILE), "Data");
        List<Map<String, String>> problems = readRows(baseDir.resolve(PROBLEMS_FILE), "Data");
        log.info(
            "Loaded business intelligence source data baseDir={} incidents={} incidentSlaCriteria={} requestSlaCriteria={} changes={} requests={} problems={}",
            baseDir,
            incidents.size(),
            incidentSlaCriteria.size(),
            requestSlaCriteria.size(),
            changes.size(),
            requests.size(),
            problems.size()
        );
        List<Map<String, String>> enrichedIncidents = enrichIncidentsWithSla(incidents, incidentSlaCriteria);
        return new BiRawData(enrichedIncidents, incidentSlaCriteria, changes, requests, problems, requestSlaCriteria);
    }

    private List<Map<String, String>> readRows(Path file, String sheetName) {
        if (!Files.exists(file)) {
            log.warn("Business intelligence data file missing file={} sheet={}", file, sheetName);
            return List.of();
        }
        try (InputStream inputStream = Files.newInputStream(file);
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                log.warn("Business intelligence sheet missing file={} sheet={}", file, sheetName);
                return List.of();
            }
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                log.warn("Business intelligence sheet header missing file={} sheet={}", file, sheetName);
                return List.of();
            }

            List<String> headers = new ArrayList<>();
            int lastCellNum = Math.max(headerRow.getLastCellNum(), 0);
            for (int cellIndex = 0; cellIndex < lastCellNum; cellIndex++) {
                headers.add(readCell(headerRow.getCell(cellIndex)));
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new LinkedHashMap<>();
                boolean hasValue = false;
                for (int cellIndex = 0; cellIndex < headers.size(); cellIndex++) {
                    String header = headers.get(cellIndex);
                    if (header == null || header.isBlank()) {
                        continue;
                    }
                    String value = readCell(row.getCell(cellIndex));
                    if (!value.isBlank()) {
                        hasValue = true;
                    }
                    values.put(header, value);
                }
                if (hasValue) {
                    rows.add(values);
                }
            }
            return rows;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read data file: " + file, exception);
        }
    }

    /**
     * Inject computed "SLA Compliant" (Yes/No) into each incident row by comparing
     * actual Response Time / Resolution Time against per-priority SLA criteria.
     */
    private List<Map<String, String>> enrichIncidentsWithSla(
            List<Map<String, String>> incidents,
            List<Map<String, String>> criteria) {
        if (criteria.isEmpty() || incidents.isEmpty()) {
            return incidents;
        }
        Map<String, Double> responseTargets = buildCriteriaMap(criteria,
            List.of("response_sla_min"));
        Map<String, Double> resolutionTargets = buildCriteriaMap(criteria,
            List.of("resolution_sla_min"));

        for (Map<String, String> row : incidents) {
            String priority = row.getOrDefault(BiColumns.PRIORITY, "").trim();
            Double respTarget = responseTargets.get(priority);
            Double resolTarget = resolutionTargets.get(priority);
            if (priority.isEmpty() || respTarget == null || resolTarget == null) {
                row.put(BiColumns.SLA_COMPLIANT, "");
            } else {
                String respRaw = row.getOrDefault(BiColumns.RESPONSE_TIME_M, "").trim();
                String resolRaw = row.getOrDefault(BiColumns.RESOLUTION_TIME_M, "").trim();
                if (respRaw.isEmpty() || resolRaw.isEmpty()) {
                    row.put(BiColumns.SLA_COMPLIANT, "");
                } else {
                    double respMinutes = parseDouble(respRaw);
                    double resolMinutes = parseDouble(resolRaw);
                    boolean met = respMinutes <= respTarget && resolMinutes <= resolTarget;
                    row.put(BiColumns.SLA_COMPLIANT, met ? "Yes" : "No");
                }
            }
        }
        return incidents;
    }

    private Map<String, Double> buildCriteriaMap(List<Map<String, String>> criteria,
                                                  List<String> candidateKeys) {
        Map<String, Double> map = new LinkedHashMap<>();
        for (Map<String, String> row : criteria) {
            String priority = row.getOrDefault(BiColumns.PRIORITY, "").trim();
            if (priority.isEmpty()) continue;
            for (String key : candidateKeys) {
                String val = row.getOrDefault(key, "").trim();
                if (!val.isEmpty()) {
                    map.put(priority, parseDouble(val));
                    break;
                }
            }
        }
        return map;
    }

    private double parseDouble(String value) {
        if (value == null || value.isBlank()) return 0;
        try {
            return Double.parseDouble(value.replaceAll("[,，]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String readCell(Cell cell) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }
}
