package com.example.Controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RestController;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.io.IOException;
import java.util.List;
import java.util.Map;



@RestController
public class ExportController {

    private final JdbcTemplate jdbcTemplate;

    public ExportController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }


//    private String getValueAsString(Map<String, Object> row, String column) {
//        Object value = row.get(column);
//        return value != null ? value.toString() : "";
//    }

    @GetMapping("/export/excel")
    public void exportToExcel(HttpServletResponse response) throws IOException {
        // Use aliased SQL query
        String sql = """
        SELECT 
            id,
            first_name AS "firstName",
            last_name AS "lastName",
            email,
            birth_date AS "birthDate"
        FROM persons
        """;
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);

        Workbook workbook = new SXSSFWorkbook();
        Sheet sheet = workbook.createSheet("Persons");

        // Headers
        String[] headers = {"ID", "First Name", "Last Name", "Email", "Birth Date"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.createCell(i).setCellValue(headers[i]);
        }

        // Data rows
        int rowNum = 1;
        for (Map<String, Object> row : data) {
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(getValueAsString(row, "id"));
            dataRow.createCell(1).setCellValue(getValueAsString(row, "firstName"));
            dataRow.createCell(2).setCellValue(getValueAsString(row, "lastName"));
            dataRow.createCell(3).setCellValue(getValueAsString(row, "email"));
            dataRow.createCell(4).setCellValue(getValueAsString(row, "birthDate"));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=persons.xlsx");
        workbook.write(response.getOutputStream());
        workbook.close();
    }

    private String getValueAsString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return (value != null) ? value.toString() : ""; // Handle nulls
    }




    @GetMapping("/export/csv")
    public void exportToCsv(HttpServletResponse response) throws IOException {
        // Use aliased SQL query
        String sql = """
                SELECT 
                    id,
                    first_name AS "firstName",
                    last_name AS "lastName",
                    email,
                    birth_date AS "birthDate"
                FROM persons
                """;
        List<Map<String, Object>> data = jdbcTemplate.queryForList(sql);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=persons.csv");

        // Headers
        String[] headers = {"ID", "First Name", "Last Name", "Email", "Birth Date"};
        response.getWriter().write(String.join(",", headers) + "\n");

        // Data rows with CSV-safe formatting
        for (Map<String, Object> row : data) {
            String line = String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"",
                    getValueAsString(row, "id"),
                    getValueAsString(row, "firstName"),
                    getValueAsString(row, "lastName"),
                    getValueAsString(row, "email"),
                    getValueAsString(row, "birthDate"));
            response.getWriter().write(line + "\n");
        }

        response.getWriter().flush();

    }
}