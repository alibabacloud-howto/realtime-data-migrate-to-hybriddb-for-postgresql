package com.alibaba.imo.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.imo.utils.DatabaseUtil;

import org.apache.commons.lang.StringUtils;

public class DbService {

    /**
     * generate Upsert sql the detail info can look at here:
     * (1)batch upsert:https://githu.com/digoal/blog/blob/master/201806/20180605_01.md
     * (2)single upsert:https://github.com/digoal/blog/blob/master/201806/20180604_01.md
     *
     * @param csvValues
     * @param triggerTableName
     * @return
     */
    public static String generateUpsertSql(List<List<String>> csvValues, String triggerTableName) {
        StringBuffer sb = new StringBuffer();
        Map<String, Map> columnMap = OssService.resultMap;
        for (Map.Entry<String, Map> entry : columnMap.entrySet()) {
            String tableFullName = entry.getKey();
            String tableSchemaName = tableFullName.split("[.]")[0];
            String tableName = tableFullName.split("[.]")[1];
            try {
                if (triggerTableName.equals(tableFullName)) {
                    sb.append("select gp_upsert_batch('");
                    String shardingKey = (String)entry.getValue().get("shardingKey");
                    String[] shardingKeyArr = shardingKey.split(",");
                    String distKeys = "";
                    for (int i = 0; i < shardingKeyArr.length; i++) {
                        if (i >= shardingKeyArr.length - 1) {
                            distKeys += "'" + shardingKeyArr[i] + "'";
                        } else {
                            distKeys += "'" + shardingKeyArr[i] + "',";
                        }
                    }
                    sb.append(tableSchemaName).append("','").append(tableName).append("',array[").append(distKeys)
                        .append("],array['");
                    List<String> columnList = (List)entry.getValue().get("attrNameStr");
                    for (int j = 0; j < csvValues.size(); j++) {
                        Map<String, Object> dataMap = new HashMap<>();
                        for (int i = 0; i < csvValues.get(j).size(); i++) {
                            if (StringUtils.isBlank(csvValues.get(j).get(i))) {
                                dataMap.put(columnList.get(i), null);
                            } else {
                                //replace ' and " that may affect the sql
                                dataMap.put(columnList.get(i), csvValues.get(j).get(i).replaceAll("'", "''"));
                            }
                        }
                        if (j >= (csvValues.size() - 1)) {
                            sb.append(JSON.toJSONString(dataMap, SerializerFeature.WriteMapNullValue)).append(
                                "'::json");
                        } else {
                            sb.append(JSON.toJSONString(dataMap, SerializerFeature.WriteMapNullValue)).append(
                                "'::json,'");
                        }
                    }
                    sb.append("]);");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    /**
     * generate insert sql for tables only insert operation
     *
     * @param csvValues
     * @param triggerTableName
     * @return
     */
    public static String generateStatementInsertSql(List<List<String>> csvValues, String triggerTableName) {
        Map<String, Map> columnMap = OssService.resultMap;
        for (Map.Entry<String, Map> entry : columnMap.entrySet()) {
            String tableFullName = entry.getKey();
            try {
                if (triggerTableName.equals(tableFullName)) {
                    List<String> columnList = (List)entry.getValue().get("attrNameStr");
                    StringBuffer sb = new StringBuffer();
                    sb.append("insert into " + tableFullName + "(");
                    String columnStr = "";
                    for (int i = 0; i < columnList.size(); i++) {
                        if (i >= (columnList.size() - 1)) {
                            columnStr += columnList.get(i) + ")";
                        } else {
                            columnStr += columnList.get(i) + ",";
                        }
                    }
                    sb.append(columnStr).append(" values");
                    for (int j = 0; j < csvValues.size(); j++) {
                        sb.append("(");
                        String valueStr = "";
                        for (int i = 0; i < csvValues.get(j).size(); i++) {
                            if (i >= (csvValues.get(j).size() - 1)) {
                                if (StringUtils.isBlank(csvValues.get(j).get(i))) {
                                    valueStr += null + ");";
                                } else {
                                    valueStr += "'" + csvValues.get(j).get(i).replaceAll("'", "''") + "')";
                                }
                            } else {
                                if (StringUtils.isBlank(csvValues.get(j).get(i))) {
                                    valueStr += null + ",";
                                } else {
                                    valueStr += "'" + csvValues.get(j).get(i).replaceAll("'", "''") + "',";
                                }
                            }
                        }
                        if (j >= csvValues.size() - 1) {
                            sb.append(valueStr).append(";");
                        } else {
                            sb.append(valueStr).append(",");
                        }
                    }
                    return sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * execute sql, connect to the pg master instance automcatically,proxy is not stable
     *
     * @param sql
     * @return
     */
    public static boolean hybridPg(String sql, boolean isInsertTable, String requestId) {
        if (StringUtils.isBlank(sql)) {
            return true;
        }
        Connection db = null;
        Statement st = null;
        boolean isSuccess = false;
        try {
            db = DatabaseUtil.getConnection("pg");
            st = db.createStatement();
            isSuccess = st.execute(sql);
            int updateCount = st.getUpdateCount();
            if (updateCount > 0 || isSuccess) {
                isSuccess = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DatabaseUtil.closeSourceConnection(db, st, null);
        }
        return isSuccess;
    }
}
