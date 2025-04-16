package com.example.Writer;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.beans.PropertyDescriptor;
import java.io.FileOutputStream;
import java.util.List;

public class ExcelItemWriter<T> implements ItemWriter<T>, InitializingBean {

    private Resource resource;
    private String sheetName;
    private String[] columnNames;
    private String[] propertyNames;
    private Workbook workbook;
    private Sheet sheet;
    private int rowCount = 0;

    @Override
    public void write(Chunk<? extends T> items) throws Exception {
        if (rowCount == 0) {
            createHeaderRow();
        }

        for (T item : items) {
            Row row = sheet.createRow(rowCount++);
            for (int i = 0; i < propertyNames.length; i++) {
                Cell cell = row.createCell(i);
                PropertyDescriptor propertyDescriptor = new PropertyDescriptor(propertyNames[i], item.getClass());
                Object value = propertyDescriptor.getReadMethod().invoke(item);
                setCellValue(cell, value);
            }
        }

        // Write to file after processing all items
        try (FileOutputStream outputStream = new FileOutputStream(resource.getFile())) {
            workbook.write(outputStream);
        }
    }

    private void createHeaderRow() {
        Row headerRow = sheet.createRow(rowCount++);
        for (int i = 0; i < columnNames.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columnNames[i]);
        }
    }

    private void setCellValue(Cell cell, Object value) {
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof String) {
            cell.setCellValue((String) value);
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Boolean) {
            cell.setCellValue((Boolean) value);
        } else if (value instanceof java.util.Date) {
            cell.setCellValue((java.util.Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(resource, "Resource must not be null");
        Assert.notNull(sheetName, "Sheet name must not be null");
        Assert.notNull(columnNames, "Column names must not be null");
        Assert.notNull(propertyNames, "Property names must not be null");
        Assert.isTrue(columnNames.length == propertyNames.length,
                "Column names and property names must have the same length");

        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet(sheetName);
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }

    public void setPropertyNames(String[] propertyNames) {
        this.propertyNames = propertyNames;
    }
}