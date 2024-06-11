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


