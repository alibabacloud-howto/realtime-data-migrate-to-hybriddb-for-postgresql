package com.alibaba.imo;

public class ConfigConstant {

    /**
     * teg database
     * DB_CONNECTION_URL must add preferQueryMode=simple
     */
    public static final String DB_DRIVER_CLASSS = "org.postgresql.Driver";
    public static final String DB_CONNECTION_PG_URL
        = "postgrey host:port/username?preferQueryMode=simple";
    public static final String DB_CONNECTION_USER = "username";
    public static final String DB_CONNECTION_PD = "password";
    public static final int DB_MAX_ACTIVE = 200;//the max size of connect pool
    public static final int DB_MAX_WAIT = 600000;//the max size of connect wait time
    public static final int DB_INIT_COUNT = 10;//the init size of connect pool
    public static final int DB_MIN_COUNT = 1;//min size of database pool
    public static final int DB_QUERY_TIMEOUT = 600;//timeout of db query
    /**
     * teg oss configure info
     */
    public static final String OSS_AKID = "XX";
    public static final String OSS_AKSK = "XX";
    public static final String OSS_ENDPOINT = "XX";

    //column_names.txt config file location, if you edit the column_names.txt, pls first download the txt file and
    // edit it directly,OSS_ERROR_FILE_LOG_PATH is when update db error, record the file name and error index into oss
    //config-other.json is other config info, dbType=pg means connect to pg directly or connect to mysql proxy
    public static final String OSS_COLUMN_CONFIG_FILE_BUCKET = "XX";
    public static final String OSS_COLUMN_CONFIG_FILE_PATH = "XX";
    public static final String OSS_ERROR_FILE_LOG_PATH = "XX";
    public static final String OSS_CONFIG_JSON_PATH = "XX";

    /**
     * local file stored in fc
     */
    public static final String CSV_PATH = "/tmp/teg_poc/";

    /**
     * each data size for batch sql
     */
    public static final int CSV_PER_SIZE = 1000;

    /**
     * insert into HDB retry count
     */

    public static final int RETRY_COUNT = 3;
    /**
     * test name, useless
     */
    public static final String sqlData = "";

    /**
     * the table only want to insert
     */
    public static final String[] INSERT_TABLES = {"staging.demo1"};

}
