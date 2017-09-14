package org.wso2.apim.sample.app.sub.migrator.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class APIMOracleDataSource {

    private HikariDataSource dataSource;

    public APIMOracleDataSource(String dataSourcePath) {
        HikariConfig config = new HikariConfig(dataSourcePath);
        dataSource = new HikariDataSource(config);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
