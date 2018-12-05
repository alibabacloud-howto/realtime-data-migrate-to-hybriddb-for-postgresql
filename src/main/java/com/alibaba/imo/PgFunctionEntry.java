package com.alibaba.imo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.imo.dto.OssObjectContextDto;
import com.alibaba.imo.service.CsvFileService;
import com.alibaba.imo.service.OssService;

import com.aliyun.fc.runtime.Context;
import com.aliyun.fc.runtime.Credentials;
import com.aliyun.fc.runtime.StreamRequestHandler;
import org.apache.commons.io.IOUtils;

public class PgFunctionEntry implements StreamRequestHandler {
    /**
     * this is the entrance of FunctionCompute
     *
     * @param inputStream
     * @param outputStream
     * @param context
     * @throws IOException
     */
    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        String inputStr = IOUtils.toString(inputStream, "utf-8");
        List<OssObjectContextDto> ossObjectContextDtoList = operateOssObject(inputStr, context);
        String csvFilePath = OssService.csvFileDownloadtoLocal(ossObjectContextDtoList);
        boolean isSuccess = CsvFileService.read(csvFilePath, ossObjectContextDtoList);
        if (!isSuccess) {//retry 3 times
            throw new IOException();
        }
        outputStream.write(new String(String.valueOf(isSuccess)).getBytes());
    }

    /**
     * change trigger inputStream String to dto,the jsonStr style you can look this file eventTxt
     *
     * @param jsonStr
     * @param context
     * @return
     */
    private List<OssObjectContextDto> operateOssObject(String jsonStr, Context context) {
        List<OssObjectContextDto> ossObjectContextDtoList = new ArrayList<>();
        Credentials credentials = context.getExecutionCredentials();
        String akId = credentials.getAccessKeyId();
        String akSecret = credentials.getAccessKeySecret();
        String secToken = credentials.getSecurityToken();
        JSONObject jsonObject = JSON.parseObject(jsonStr);
        JSONArray jsonArray = JSON.parseArray(jsonObject.getString("events"));
        for (int i = 0; i < jsonArray.size(); i++) {
            OssObjectContextDto ossObjectContextDto = new OssObjectContextDto();
            ossObjectContextDto.setAkId(akId);
            ossObjectContextDto.setAkSecret(akSecret);
            ossObjectContextDto.setSecToken(secToken);
            JSONObject jsonObject1 = jsonArray.getJSONObject(i);
            JSONObject ossObject = jsonObject1.getJSONObject("oss");
            JSONObject ossBucketObject = ossObject.getJSONObject("bucket");
            String bucketName = ossBucketObject.getString("name");
            ossObjectContextDto.setBucketName(bucketName);
            JSONObject ossObjObject = ossObject.getJSONObject("object");
            String objectName = ossObjObject.getString("key");
            ossObjectContextDto.setObjectName(objectName);

            JSONObject requestObject = jsonObject1.getJSONObject("responseElements");
            ossObjectContextDto.setRequestId(requestObject.getString("requestId"));

            ossObjectContextDtoList.add(ossObjectContextDto);
        }
        return ossObjectContextDtoList;
    }

}