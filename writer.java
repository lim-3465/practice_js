import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExcelUpdater {

    public void updateExcelFile(Path filePath, List<Map<String, String>> data, String adjacentColumnName) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                headerRow = sheet.createRow(0);
            }

            // Find the index of the adjacent column
            int adjacentColumnIndex = findColumnIndex(headerRow, adjacentColumnName);

            // Create or find new headers next to the adjacent column
            Map<String, Integer> newHeaderIndexMap = createOrFindNewHeaders(headerRow, data, adjacentColumnIndex);

            // Update rows with data
            int rowIndex = 1; // Start from the second row
            for (Map<String, String> rowData : data) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    row = sheet.createRow(rowIndex);
                }
                for (Map.Entry<String, String> entry : rowData.entrySet()) {
                    int columnIndex = newHeaderIndexMap.get(entry.getKey());
                    Cell cell = row.getCell(columnIndex, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                    cell.setCellValue(entry.getValue());
                }
                rowIndex++;
            }

            try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
                workbook.write(fos);
            }
        }
    }

    private int findColumnIndex(Row headerRow, String columnName) {
        for (Cell cell : headerRow) {
            if (cell.getStringCellValue().equals(columnName)) {
                return cell.getColumnIndex();
            }
        }
        throw new IllegalArgumentException("Column " + columnName + " not found");
    }

    private Map<String, Integer> createOrFindNewHeaders(Row headerRow, List<Map<String, String>> data, int adjacentColumnIndex) {
        Map<String, Integer> headerIndexMap = new HashMap<>();
        int newColumnIndex = adjacentColumnIndex + 1;

        for (String key : data.get(0).keySet()) {
            Cell cell = null;
            for (Cell existingCell : headerRow) {
                if (existingCell.getStringCellValue().equals(key)) {
                    cell = existingCell;
                    break;
                }
            }
            if (cell == null) {
                cell = headerRow.createCell(newColumnIndex);
                cell.setCellValue(key);
                headerIndexMap.put(key, newColumnIndex);
                newColumnIndex++;
            } else {
                headerIndexMap.put(key, cell.getColumnIndex());
            }
        }
        return headerIndexMap;
    }
}


















import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class CsvUpdater {

    public void updateCsvFile(Path filePath, List<Map<String, String>> data, String adjacentColumnName) throws IOException, CsvException {
        List<String[]> allData;

        // Read existing data from CSV file
        try (CSVReader reader = new CSVReader(new FileReader(filePath.toFile()))) {
            allData = reader.readAll();
        }

        // Check if headers exist and find the column index
        String[] headers = allData.isEmpty() ? new String[0] : allData.get(0);
        int adjacentColumnIndex = findColumnIndex(headers, adjacentColumnName);
        Map<String, Integer> newHeaderIndexMap = createOrFindNewHeaders(headers, data, adjacentColumnIndex);

        // Update rows with data
        int rowIndex = 1; // Start from the second row
        for (Map<String, String> rowData : data) {
            String[] row = rowIndex < allData.size() ? allData.get(rowIndex) : new String[headers.length];
            for (Map.Entry<String, String> entry : rowData.entrySet()) {
                int columnIndex = newHeaderIndexMap.get(entry.getKey());
                row = ensureCapacity(row, columnIndex + 1);
                row[columnIndex] = entry.getValue();
            }
            if (rowIndex < allData.size()) {
                allData.set(rowIndex, row);
            } else {
                allData.add(row);
            }
            rowIndex++;
        }

        // Write updated data back to CSV file
        try (CSVWriter writer = new CSVWriter(new FileWriter(filePath.toFile()))) {
            writer.writeAll(allData);
        }
    }

    private int findColumnIndex(String[] headers, String columnName) {
        return IntStream.range(0, headers.length)
                .filter(i -> headers[i].equals(columnName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found"));
    }

    private Map<String, Integer> createOrFindNewHeaders(String[] headers, List<Map<String, String>> data, int adjacentColumnIndex) {
        Map<String, Integer> headerIndexMap = new HashMap<>();
        int newColumnIndex = adjacentColumnIndex + 1;

        for (String key : data.get(0).keySet()) {
            int index = IntStream.range(0, headers.length)
                                 .filter(i -> headers[i].equals(key))
                                 .findFirst()
                                 .orElse(-1);
            if (index == -1) {
                headers = ensureCapacity(headers, newColumnIndex + 1);
                headers[newColumnIndex] = key;
                headerIndexMap.put(key, newColumnIndex);
                newColumnIndex++;
            } else {
                headerIndexMap.put(key, index);
            }
        }

        return headerIndexMap;
    }

    private String[] ensureCapacity(String[] array, int minCapacity) {
        if (array.length < minCapacity) {
            String[] newArray = new String[minCapacity];
            System.arraycopy(array, 0, newArray, 0, array.length);
            return newArray;
        }
        return array;
    }
}























import com.opencsv.exceptions.CsvException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Main implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        ExcelUpdater excelUpdater = new ExcelUpdater();
        CsvUpdater csvUpdater = new CsvUpdater();

        // Get the list of users from the database and convert to List<Map<String, String>>
        List<User> users = userService.getAllUsers();
        List<Map<String, String>> data = users.stream().map(user -> {
            Map<String, String> map = new HashMap<>();
            map.put("ID", user.getId().toString());
            map.put("Name", user.getName());
            map.put("Email", user.getEmail());
            return map;
        }).collect(Collectors.toList());

        String adjacentColumnName = "Name"; // Example column name

        try {
            // Update the Excel file with data
            Path excelPath = Paths.get("path/to/your/file.xlsx");
            excelUpdater.updateExcelFile(excelPath, data, adjacentColumnName);

            // Update the CSV file with data
            Path csvPath = Paths.get("path/to/your/file.csv");
            csvUpdater.updateCsvFile(csvPath, data, adjacentColumnName);

        } catch (IOException | CsvException e) {
            e.printStackTrace();
        }
    }
}




















import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvUpdater {

    public void updateCsvFile(Path filePath, List<Map<String, String>> data, String adjacentColumnName) throws IOException {
        // Read existing data from CSV file
        BufferedReader reader = new BufferedReader(new FileReader(filePath.toFile()));
        CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader());
        List<CSVRecord> records = parser.getRecords();
        Map<String, Integer> headerMap = parser.getHeaderMap();
        
        // Check if headers exist and find the adjacent column index
        if (!headerMap.containsKey(adjacentColumnName)) {
            throw new IllegalArgumentException("Column " + adjacentColumnName + " not found");
        }
        int adjacentColumnIndex = headerMap.get(adjacentColumnName);

        // Create or find new headers next to the adjacent column
        Map<String, Integer> newHeaderIndexMap = createOrFindNewHeaders(headerMap, data, adjacentColumnIndex);

        // Prepare new CSV format with updated headers
        String[] newHeaders = new String[newHeaderIndexMap.size()];
        for (Map.Entry<String, Integer> entry : newHeaderIndexMap.entrySet()) {
            newHeaders[entry.getValue()] = entry.getKey();
        }
        CSVFormat updatedFormat = CSVFormat.DEFAULT.withHeader(newHeaders);

        // Update rows with data
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()));
        CSVPrinter printer = new CSVPrinter(writer, updatedFormat);

        // Write header
        printer.printRecord((Object[]) newHeaders);

        // Write existing and new data
        for (int i = 0; i < records.size(); i++) {
            CSVRecord record = records.get(i);
            Map<String, String> rowData = i < data.size() ? data.get(i) : new HashMap<>();
            String[] newRow = new String[newHeaderIndexMap.size()];

            // Copy existing data
            for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
                newRow[entry.getValue()] = record.get(entry.getKey());
            }

            // Add new data
            for (Map.Entry<String, String> entry : rowData.entrySet()) {
                newRow[newHeaderIndexMap.get(entry.getKey())] = entry.getValue();
            }

            printer.printRecord((Object[]) newRow);
        }

        printer.flush();
        printer.close();
        parser.close();
        reader.close();
    }

    private Map<String, Integer> createOrFindNewHeaders(Map<String, Integer> headerMap, List<Map<String, String>> data, int adjacentColumnIndex) {
        Map<String, Integer> newHeaderIndexMap = new HashMap<>(headerMap);
        int newColumnIndex = adjacentColumnIndex + 1;

        for (String key : data.get(0).keySet()) {
            if (!headerMap.containsKey(key)) {
                newHeaderIndexMap.put(key, newColumnIndex);
                newColumnIndex++;
            }
        }
        return newHeaderIndexMap;
    }
}




package com.example.demo;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class FileDownloadController {

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile() {
        try {
            Path filePath = Paths.get("path/to/your/file.txt").toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() || resource.isReadable()) {
                String contentType = "application/octet-stream";

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw new RuntimeException("File not found or not readable");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while reading file", e);
        }
    }
}

