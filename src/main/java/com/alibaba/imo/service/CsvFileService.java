package com.alibaba.imo.service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.alibaba.imo.ConfigConstant;
import com.alibaba.imo.dto.OssObjectContextDto;
import com.alibaba.imo.utils.FileExecutorUtil;

import com.csvreader.CsvReader;

public class CsvFileService {
    /**
     * read data from csv file, each operate count depends on ConfigConstant.CSV_PER_SIZE
     *
     * @param filePath
     * @param ossObjectContextDtoList
     * @return
     */
    public static boolean read(String filePath, List<OssObjectContextDto> ossObjectContextDtoList) {
        try {
            String requestId = ossObjectContextDtoList.get(0).getRequestId();
            String tableFullName = "";
            if (ossObjectContextDtoList.get(0).getObjectName().startsWith("test")) {
                tableFullName = ossObjectContextDtoList.get(0).getObjectName().split("/")[1];
            } else {
                tableFullName = ossObjectContextDtoList.get(0).getObjectName().split("/")[0];
            }
            //create csv object
            CsvReader csvReader = new CsvReader(filePath);
            // read table header
            //csvReader.readHeaders();
            List<List<String>> valuesList = new LinkedList<>();
            Long startIndex = csvReader.getCurrentRecord();
            Long currentIndex = csvReader.getCurrentRecord();
            boolean isSuccess = true;
            int flag = 0;
            boolean isInsertTable = isInsertTable(tableFullName);
            while (csvReader.readRecord()) {
                String[] values = csvReader.getValues();
                List<String> valuesMidList = Arrays.asList(values);
                valuesList.add(valuesMidList);
                currentIndex = csvReader.getCurrentRecord();
                if ((currentIndex - startIndex) >= ConfigConstant.CSV_PER_SIZE) {
                    if (isInsertTable) {
                        isSuccess = isSuccess && DbService.hybridPg(
                            DbService.generateStatementInsertSql(valuesList, tableFullName), isInsertTable, requestId);
                    } else {
                        isSuccess = isSuccess && DbService.hybridPg(
                            DbService.generateUpsertSql(valuesList, tableFullName), isInsertTable, requestId);
                    }
                    startIndex = csvReader.getCurrentRecord();
                    valuesList.clear();
                    continue;
                }
            }
            //last part , can not reach to CSV_PER_SIZE
            if ((!currentIndex.equals(startIndex) && (currentIndex - startIndex) < ConfigConstant.CSV_PER_SIZE)) {
                if (isInsertTable) {
                    isSuccess = isSuccess && DbService.hybridPg(
                        DbService.generateStatementInsertSql(valuesList, tableFullName), isInsertTable, requestId);
                } else {
                    isSuccess = isSuccess && DbService.hybridPg(
                        DbService.generateUpsertSql(valuesList, tableFullName), isInsertTable, requestId);
                }
            }
            FileExecutorUtil.deleteFile(new File(filePath));
            //insert tables can not retry all of the csv datas
            if (isInsertTable && !isSuccess) {
                isSuccess = true;
            }
            return isSuccess;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean isInsertTable(String tableFullName) {
        boolean isInsertTable = false;
        try {
            for (String tableName : ConfigConstant.INSERT_TABLES) {
                if (tableFullName.equals(tableName)) {
                    isInsertTable = true;
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("whether the table is only insert is wrong");
        }
        return isInsertTable;
    }

}
