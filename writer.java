




Jenkins와 Bitbucket, Windows PowerShell, JFrog Artifactory (AIM)를 사용하여 배포하는 방법에 대해 단계별로 설명드리겠습니다. 아래 단계는 Jenkins를 사용하여 Bitbucket에서 소스 코드를 가져오고, Windows PowerShell 스크립트를 사용하여 AIM으로 배포하는 과정을 포함합니다.

### 1. 환경 준비

먼저 필요한 도구들을 설치하고 설정합니다.

- Jenkins: CI/CD 도구
- Bitbucket: 소스 코드 저장소
- JFrog Artifactory (AIM): 아티팩트 저장소
- Windows PowerShell: 스크립트 실행

### 2. Bitbucket 리포지토리 설정

Bitbucket에 프로젝트 리포지토리를 설정하고, Jenkins에서 접근할 수 있도록 설정합니다.

1. Bitbucket에서 새로운 리포지토리를 생성합니다.
2. 리포지토리 URL을 복사합니다. (예: `https://bitbucket.org/your-username/your-repo.git`)

### 3. Jenkins 설정

Jenkins를 설치하고 설정합니다. Jenkins는 기본적으로 웹 브라우저를 통해 관리할 수 있습니다.

#### Jenkins 플러그인 설치

Jenkins에서 필요한 플러그인을 설치합니다.

1. Jenkins 대시보드에서 `Manage Jenkins` > `Manage Plugins`로 이동합니다.
2. `Available` 탭에서 다음 플러그인을 검색하고 설치합니다:
   - `Git Plugin`
   - `Bitbucket Plugin`
   - `Pipeline`
   - `JFrog Artifactory Plugin`

#### Jenkins 파이프라인 생성

1. Jenkins 대시보드에서 `New Item`을 클릭합니다.
2. `Pipeline`을 선택하고, 이름을 입력한 후 `OK`를 클릭합니다.

### 4. Jenkins 파이프라인 설정

파이프라인 스크립트를 작성하여 Bitbucket에서 코드를 가져오고 PowerShell 스크립트를 실행하여 AIM으로 배포합니다.

#### Jenkinsfile

프로젝트의 루트 디렉토리에 `Jenkinsfile`을 작성합니다.

```groovy
pipeline {
    agent any

    environment {
        BITBUCKET_CREDENTIALS = credentials('bitbucket-credentials-id')
        ARTIFACTORY_CREDENTIALS = credentials('artifactory-credentials-id')
    }

    stages {
        stage('Checkout') {
            steps {
                git url: 'https://bitbucket.org/your-username/your-repo.git',
                    credentialsId: 'bitbucket-credentials-id'
            }
        }

        stage('Build') {
            steps {
                powershell './build.ps1'
            }
        }

        stage('Deploy') {
            steps {
                script {
                    def server = Artifactory.server 'artifactory-server-id'
                    def uploadSpec = """{
                        "files": [
                            {
                                "pattern": "build/output/**/*",
                                "target": "your-repo/your-folder/"
                            }
                        ]
                    }"""
                    server.upload(uploadSpec)
                }
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'build/output/**/*', allowEmptyArchive: true
        }
    }
}
```

### 5. PowerShell 스크립트 작성

프로젝트 루트 디렉토리에 PowerShell 스크립트를 작성합니다.

#### build.ps1

```powershell
# build.ps1

# Clean previous builds
Remove-Item -Recurse -Force build/output/*

# Your build commands here
# For example, if you are using a .NET project:
# dotnet build

# Create an output directory if it doesn't exist
if (-Not (Test-Path -Path build/output)) {
    New-Item -ItemType Directory -Path build/output
}

# Move build artifacts to the output directory
# For example:
# Copy-Item -Path bin/Release/* -Destination build/output
```

### 6. Jenkins Credentials 설정

Jenkins에서 Bitbucket과 Artifactory 접근을 위한 자격 증명을 설정합니다.

1. Jenkins 대시보드에서 `Manage Jenkins` > `Manage Credentials`로 이동합니다.
2. `(global)` 영역에 다음 자격 증명을 추가합니다:
   - Bitbucket 자격 증명 (사용자 이름 및 비밀번호 또는 개인 액세스 토큰)
   - Artifactory 자격 증명 (사용자 이름 및 API 키)

### 7. 파이프라인 실행

1. Jenkins에서 방금 생성한 파이프라인으로 이동합니다.
2. `Build Now` 버튼을 클릭하여 파이프라인을 실행합니다.

### 요약

1. **Bitbucket 리포지토리 설정**: Bitbucket에서 리포지토리를 생성하고 URL을 복사합니다.
2. **Jenkins 설정**: Jenkins를 설치하고 필요한 플러그인을 설치합니다.
3. **Jenkins 파이프라인 생성**: 새로운 파이프라인을 생성하고 `Jenkinsfile`을 작성합니다.
4. **PowerShell 스크립트 작성**: 빌드를 위한 PowerShell 스크립트를 작성합니다.
5. **Jenkins Credentials 설정**: Jenkins에서 Bitbucket과 Artifactory 접근을 위한 자격 증명을 설정합니다.
6. **파이프라인 실행**: Jenkins에서 파이프라인을 실행하여 Bitbucket에서 코드를 가져오고, 빌드하고, Artifactory에 배포합니다.

이 단계를 따라하면 Jenkins와 Bitbucket, PowerShell, Artifactory를 사용하여 소스 코드를 배포할 수 있습니다.







맵의 키 값과 객체의 필드 이름이 다를 때, 이를 매핑하여 변환하는 유틸리티 메서드를 작성할 수 있습니다. 이 메서드는 주어진 매핑 정보를 사용하여 맵의 데이터를 객체의 필드로 설정합니다.

다음은 Java에서 이 작업을 수행하는 방법에 대한 예제입니다.

### 1. 객체 정의

먼저, 데이터를 매핑할 객체 클래스를 정의합니다.

#### Person.java

```java
package com.example.demo;

public class Person {
    private String fullName;
    private String emailAddress;

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String toString() {
        return "Person{fullName='" + fullName + "', emailAddress='" + emailAddress + "'}";
    }
}
```

### 2. Map을 객체로 변환하는 유틸리티 클래스

리플렉션을 사용하여 Map의 데이터를 객체로 변환하는 유틸리티 메서드를 정의합니다.

#### ObjectMapperUtils.java

```java
package com.example.demo.utils;

import java.lang.reflect.Field;
import java.util.Map;

public class ObjectMapperUtils {

    public static <T> T mapToEntity(Map<String, String> map, Class<T> clazz, Map<String, String> fieldMappings) throws IllegalAccessException, InstantiationException {
        T entity = clazz.newInstance();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String fieldName = fieldMappings.getOrDefault(entry.getKey(), entry.getKey());
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(entity, entry.getValue());
            } catch (NoSuchFieldException e) {
                // If the field does not exist, ignore it
            }
        }

        return entity;
    }
}
```

### 3. 사용 예제

매핑 정보를 사용하여 Map 데이터를 Person 객체로 변환하는 예제입니다.

#### Main.java

```java
package com.example.demo;

import com.example.demo.utils.ObjectMapperUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        List<Map<String, String>> mapData = List.of(
            Map.of("name", "antony", "mail", "antony@naver.com"),
            Map.of("name", "lim", "mail", "lim@naver.com")
        );

        // Define the field mappings
        Map<String, String> fieldMappings = Map.of(
            "name", "fullName",
            "mail", "emailAddress"
        );

        List<Person> data = mapData.stream()
            .map(map -> {
                try {
                    return ObjectMapperUtils.mapToEntity(map, Person.class, fieldMappings);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .collect(Collectors.toList());

        data.forEach(System.out::println);
    }
}
```

### 설명

1. **객체 정의**: `Person` 클래스는 `fullName`과 `emailAddress` 필드를 가집니다.
2. **유틸리티 클래스**: `ObjectMapperUtils` 클래스는 Map 데이터를 객체로 변환합니다. `fieldMappings`를 사용하여 맵의 키와 객체의 필드 이름을 매핑합니다.
3. **사용 예제**: `Main` 클래스에서 맵 데이터를 `Person` 객체로 변환하고 출력합니다.

이 방법을 사용하면 맵의 키와 객체의 필드 이름이 다를 때도 데이터를 객체로 변환할 수 있습니다. `fieldMappings`를 사용하여 유연하게 매핑을 정의할 수 있습니다.






import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) {
        // 원본 맵 생성
        Map<String, String> map = new HashMap<>();
        map.put("name", "antony");
        map.put("mail", "antony@naver.com");
        map.put("age", "25");
        map.put("city", "Seoul");

        // 유지할 키들 정의
        Set<String> keysToKeep = new HashSet<>();
        keysToKeep.add("name");
        keysToKeep.add("mail");

        // 맵에서 원하는 키만 남기기
        map.keySet().retainAll(keysToKeep);

        // 결과 출력
        System.out.println(map); // {name=antony, mail=antony@naver.com}
    }
}



import React, { useState, useEffect, useMemo } from 'react';
import { AgGridReact } from 'ag-grid-react';
import 'ag-grid-community/styles/ag-grid.css';
import 'ag-grid-community/styles/ag-theme-alpine.css';

const App = () => {
  const [rowData, setRowData] = useState([]);

  const fetchData = async () => {
    const response = await fetch('https://api.example.com/data'); // API URL을 여기에 입력하세요
    const data = await response.json();
    setRowData(data);
  };

  useEffect(() => {
    fetchData();
  }, []);

  const ButtonRenderer = (params) => {
    return (
      <button onClick={() => alert(`Row data: ${JSON.stringify(params.data)}`)}>
        Click Me
      </button>
    );
  };

  const columnDefs = useMemo(() => [
    { headerName: 'Name', field: 'name' },
    { headerName: 'Email', field: 'email' },
    {
      headerName: 'Actions',
      field: 'actions',
      cellRendererFramework: ButtonRenderer
    }
  ], []);

  return (
    <div className="ag-theme-alpine" style={{ height: 400, width: 600 }}>
      <AgGridReact
        rowData={rowData}
        columnDefs={columnDefs}
        defaultColDef={{ flex: 1, minWidth: 150, resizable: true }}
        suppressRowClickSelection={true}
        rowSelection="none"
      />
    </div>
  );
};

export default App;





컬럼 이름과 객체의 필드 이름이 다를 경우, 이를 매핑하기 위해 매핑 정보를 사용할 수 있습니다. 매핑 정보는 주로 해시맵을 사용하여 각 컬럼 이름을 객체 필드 이름에 매핑합니다.

아래는 매핑 정보를 사용하여 `Map` 데이터를 객체로 변환하는 예제입니다.

### 1. 객체 정의

#### Person.java

```java
package com.example.demo;

public class Person {
    private String fullName;
    private String emailAddress;

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public String toString() {
        return "Person{fullName='" + fullName + "', emailAddress='" + emailAddress + "'}";
    }
}
```

### 2. Map을 객체로 변환하는 유틸리티 클래스

#### ObjectMapperUtils.java

```java
package com.example.demo.utils;

import java.lang.reflect.Field;
import java.util.Map;

public class ObjectMapperUtils {

    public static <T> T mapToEntity(Map<String, String> map, Class<T> clazz, Map<String, String> fieldMappings) throws IllegalAccessException, InstantiationException {
        T entity = clazz.newInstance();

        for (Map.Entry<String, String> entry : map.entrySet()) {
            String fieldName = fieldMappings.getOrDefault(entry.getKey(), entry.getKey());
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(entity, entry.getValue());
            } catch (NoSuchFieldException e) {
                // If the field does not exist, ignore it
            }
        }

        return entity;
    }
}
```

### 3. 사용 예제

매핑 정보를 사용하여 Map 데이터를 Person 객체로 변환하는 예제입니다.

#### Main.java

```java
package com.example.demo;

import com.example.demo.utils.ObjectMapperUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        List<Map<String, String>> mapData = List.of(
            Map.of("name", "antony", "mail", "antony@naver.com"),
            Map.of("name", "lim", "mail", "lim@naver.com")
        );

        // Define the field mappings
        Map<String, String> fieldMappings = Map.of(
            "name", "fullName",
            "mail", "emailAddress"
        );

        List<Person> data = mapData.stream()
            .map(map -> {
                try {
                    return ObjectMapperUtils.mapToEntity(map, Person.class, fieldMappings);
                } catch (InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            })
            .collect(Collectors.toList());

        data.forEach(System.out::println);
    }
}
```

### 4. 엑셀 및 CSV 파일에 객체 데이터 추가

이제 Map을 객체로 변환한 후, 객체 데이터를 엑셀 및 CSV 파일에 추가할 수 있습니다. 아래는 수정된 `ExcelUtils` 클래스입니다.

#### ExcelUtils.java (수정)

```java
package com.example.demo.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class ExcelUtils {

    public static <T> void writeDataToExcel(List<T> data, String filePath, Class<T> clazz) throws IOException, IllegalAccessException {
        XSSFWorkbook workbook;
        XSSFSheet sheet;
        boolean fileExists = new java.io.File(filePath).exists();
        Map<String, Integer> headerIndexMap = new LinkedHashMap<>();
        int rowIndex = 0;

        if (fileExists) {
            try (FileInputStream fileIn = new FileInputStream(filePath)) {
                workbook = new XSSFWorkbook(fileIn);
                sheet = workbook.getSheetAt(0);
                rowIndex = sheet.getLastRowNum() + 1;

                // Read existing headers
                Row headerRow = sheet.getRow(0);
                for (Cell cell : headerRow) {
                    headerIndexMap.put(cell.getStringCellValue(), cell.getColumnIndex());
                }
            }
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Data");
            rowIndex = 1; // Start writing data from the second row (first row is header)
        }

        // Determine all headers (existing + new)
        for (T entity : data) {
            for (Field field : clazz.getDeclaredFields()) {
                if (!headerIndexMap.containsKey(field.getName())) {
                    headerIndexMap.put(field.getName(), headerIndexMap.size());
                }
            }
        }

        // Write headers if the file did not exist
        if (!fileExists) {
            Row headerRow = sheet.createRow(0);
            for (Map.Entry<String, Integer> entry : headerIndexMap.entrySet()) {
                Cell cell = headerRow.createCell(entry.getValue());
                cell.setCellValue(entry.getKey());
            }
        } else {
            // Update headers if there are new headers
            Row headerRow = sheet.getRow(0);
            int headerCellIndex = headerRow.getLastCellNum();
            for (Map.Entry<String, Integer> entry : headerIndexMap.entrySet()) {
                if (headerRow.getCell(entry.getValue()) == null) {
                    Cell cell = headerRow.createCell(headerCellIndex++);
                    cell.setCellValue(entry.getKey());
                }
            }
        }

        // Write data rows
        for (T entity : data) {
            Row row = sheet.createRow(rowIndex++);
            for (Map.Entry<String, Integer> entry : headerIndexMap.entrySet()) {
                Cell cell = row.createCell(entry.getValue());
                Field field = clazz.getDeclaredField(entry.getKey());
                field.setAccessible(true);
                cell.setCellValue(field.get(entity).toString());
            }
        }

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }
        workbook.close();
    }
}
```

### 사용 예제

#### 엑셀 파일에 데이터 추가

```java
List<Map<String, String>> mapData = List.of(
    Map.of("name", "antony", "mail", "antony@naver.com"),
    Map.of("name", "lim", "mail", "lim@naver.com")
);

Map<String, String> fieldMappings = Map.of(
    "name", "fullName",
    "mail", "emailAddress"
);

List<Person> data = mapData.stream()
    .map(map -> {
        try {
            return ObjectMapperUtils.mapToEntity(map, Person.class, fieldMappings);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    })
    .collect(Collectors.toList());

String excelFilePath = "data.xlsx";
ExcelUtils.writeDataToExcel(data, excelFilePath, Person.class);
```

이렇게 하면 컬럼 이름과 객체 필드 이름이 다를 경우에도 Map 데이터를 객체로 변환한 후, 객체 데이터를 엑셀 파일에 추가할 수 있습니다. 동일한 방식으로 CSV 파일에도 추가할 수 있습니다.





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

