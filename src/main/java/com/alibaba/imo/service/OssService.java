package com.alibaba.imo.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.imo.ConfigConstant;
import com.alibaba.imo.dto.OssObjectContextDto;
import com.alibaba.imo.utils.FileExecutorUtil;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GetObjectRequest;
import com.aliyun.oss.model.OSSObject;

public class OssService {

    public static Map<String, Map> resultMap = new HashMap<>();

    static {
        initColumnMap();
    }

    /**
     * download csv file to local system
     *
     * @param ossObjectContextDtoList
     * @return
     */
    public static String csvFileDownloadtoLocal(List<OssObjectContextDto> ossObjectContextDtoList) {
        String endpoint = ConfigConstant.OSS_ENDPOINT;
        String ak = ConfigConstant.OSS_AKID;
        String aksk = ConfigConstant.OSS_AKSK;
        String bucketName = ossObjectContextDtoList.get(0).getBucketName();
        String objectName = ossObjectContextDtoList.get(0).getObjectName();
        // create OSSClient instance。
        OSSClient ossClient = new OSSClient(endpoint, ak, aksk);
        // download file to local file system
        String csvPath = ConfigConstant.CSV_PATH + objectName;
        try {
            FileExecutorUtil.deleteFile(new File(csvPath));
            FileExecutorUtil.createFile(new File(csvPath));
            ossClient.getObject(new GetObjectRequest(bucketName, objectName), new File(csvPath));
            // close OSSClient。
            ossClient.shutdown();
            return csvPath;
        } catch (Exception e) {
            ossClient.shutdown();
            e.printStackTrace();
        }
        return null;
    }

    private static void initColumnMap() {
        if (null != resultMap && (!resultMap.isEmpty())) {
            return;
        }
        String endpoint = ConfigConstant.OSS_ENDPOINT;
        String ak = ConfigConstant.OSS_AKID;
        String aksk = ConfigConstant.OSS_AKSK;
        String bucketName = ConfigConstant.OSS_COLUMN_CONFIG_FILE_BUCKET;
        String objectName = ConfigConstant.OSS_COLUMN_CONFIG_FILE_PATH;
        OSSClient ossClient = new OSSClient(endpoint, ak, aksk);
        OSSObject ossObject = ossClient.getObject(bucketName, objectName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(ossObject.getObjectContent()));
        try {
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                assembleMap(line);
            }
            reader.close();
            ossClient.shutdown();
        } catch (Exception e) {
            try {
                reader.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            ossClient.shutdown();
        }
    }

    private static void assembleMap(String line) {
        try {
            String[] namesSplit = line.split(":");
            if (null != namesSplit) {
                String nameStr = namesSplit[1];
                List<String> nameList = Arrays.asList(nameStr.split(","));
                Map midMap = new HashMap();
                midMap.put("attrNameStr", nameList);
                midMap.put("shardingKey", namesSplit[2]);
                resultMap.put(namesSplit[0], midMap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

    }
}
