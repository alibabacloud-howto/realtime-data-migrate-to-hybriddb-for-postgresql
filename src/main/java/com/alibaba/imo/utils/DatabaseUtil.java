package com.alibaba.imo.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.imo.ConfigConstant;

/*
 * druid connect pool util
 */
public class DatabaseUtil {

    /**
     * index of DruidDataSource
     */
    private static DataSource druidPgPool;

    private static void triggerInitPgDataSource() {
        try {
            druidPgPool = initDataSource(ConfigConstant.DB_CONNECTION_PG_URL);
            System.out.println("init pg datasource end");
        } catch (Exception e) {
            System.out.println("init pg proxy datasource error");
            e.printStackTrace();
        }
    }

    private static DruidDataSource initDataSource(String dbUrl) throws Exception {
        DruidDataSource druidDataSource = new DruidDataSource();
        druidDataSource.setUsername(ConfigConstant.DB_CONNECTION_USER);
        druidDataSource.setPassword(ConfigConstant.DB_CONNECTION_PD);
        druidDataSource.setUrl(dbUrl);
        druidDataSource.setDriverClassName(ConfigConstant.DB_DRIVER_CLASSS);
        druidDataSource.setMaxActive(ConfigConstant.DB_MAX_ACTIVE);
        druidDataSource.setInitialSize(ConfigConstant.DB_INIT_COUNT);
        druidDataSource.setMaxWait(ConfigConstant.DB_MAX_WAIT);
        druidDataSource.setMinIdle(ConfigConstant.DB_MIN_COUNT);
        druidDataSource.setQueryTimeout(ConfigConstant.DB_QUERY_TIMEOUT);
        return druidDataSource;
    }

    public static Connection getConnection(String type) {
        Connection connection = null;
        try {

            connection = getPgDruidPool().getConnection();
            return connection;
        } catch (SQLException e) {
            System.out.println("get Connection error");

        }
        return connection;
    }

    /**
     * close connect object
     */
    public static void closeSourceConnection(Connection connection, Statement statement, ResultSet resultSet) {

        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("close connection error");
            e.printStackTrace();
        }
    }

    public static DataSource getPgDruidPool() {
        if (null == druidPgPool) {
            triggerInitPgDataSource();
        }
        return druidPgPool;
    }
}