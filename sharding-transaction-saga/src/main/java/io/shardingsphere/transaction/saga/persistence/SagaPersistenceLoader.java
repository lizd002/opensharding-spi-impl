/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.transaction.saga.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.shardingsphere.core.constant.DatabaseType;
import io.shardingsphere.core.exception.ShardingException;
import io.shardingsphere.core.metadata.datasource.dialect.MySQLDataSourceMetaData;
import io.shardingsphere.transaction.saga.config.SagaPersistenceConfiguration;
import io.shardingsphere.transaction.saga.persistence.impl.EmptySagaPersistence;
import io.shardingsphere.transaction.saga.persistence.impl.jdbc.JDBCSagaPersistence;

import javax.sql.DataSource;
import java.util.ServiceLoader;

/**
 * Saga persistence loader.
 *
 * @author yangyi
 */
public final class SagaPersistenceLoader {
    
    /**
     * Load saga persistence.
     *
     * @param persistenceConfiguration persistence configuration
     * @return saga persistence
     */
    public static SagaPersistence load(final SagaPersistenceConfiguration persistenceConfiguration) {
        if (!persistenceConfiguration.isEnablePersistence()) {
            return new EmptySagaPersistence();
        }
        SagaPersistence result = null;
        for (SagaPersistence each : ServiceLoader.load(SagaPersistence.class)) {
            result = each;
        }
        if (null == result) {
            result = loadDefaultPersistence(persistenceConfiguration);
        }
        return result;
    }
    
    private static SagaPersistence loadDefaultPersistence(final SagaPersistenceConfiguration persistenceConfiguration) {
        DatabaseType databaseType = judgeDatabaseType(persistenceConfiguration.getUrl());
        String driverClassName = getJDBCDriverClassName(databaseType);
        return new JDBCSagaPersistence(initDataSource(driverClassName, persistenceConfiguration));
    }
    
    private static DatabaseType judgeDatabaseType(final String url) {
        try {
            new MySQLDataSourceMetaData(url);
            return DatabaseType.MySQL;
        } catch (final ShardingException ignore) {
        }
        throw new UnsupportedOperationException(String.format("Cannot support url `%s`", url));
    }
    
    private static String getJDBCDriverClassName(final DatabaseType databaseType) {
        switch (databaseType) {
            case MySQL:
                return "com.mysql.jdbc.Driver";
            default:
                return "";
        }
    }
    
    private static DataSource initDataSource(final String driverClassName, final SagaPersistenceConfiguration persistenceConfiguration) {
        HikariConfig config = new HikariConfig();
        validateDriverClassName(driverClassName);
        config.setDriverClassName(driverClassName);
        config.setJdbcUrl(persistenceConfiguration.getUrl());
        config.setUsername(persistenceConfiguration.getUsername());
        config.setPassword(persistenceConfiguration.getPassword());
        config.setConnectionTimeout(persistenceConfiguration.getConnectionTimeoutMilliseconds());
        config.setIdleTimeout(persistenceConfiguration.getIdleTimeoutMilliseconds());
        config.setMaxLifetime(persistenceConfiguration.getMaxLifetimeMilliseconds());
        config.setMaximumPoolSize(persistenceConfiguration.getMaxPoolSize());
        config.setMinimumIdle(persistenceConfiguration.getMinPoolSize());
        config.addDataSourceProperty("useServerPrepStmts", Boolean.TRUE.toString());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", 250);
        config.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        config.addDataSourceProperty("useLocalSessionState", Boolean.TRUE.toString());
        config.addDataSourceProperty("rewriteBatchedStatements", Boolean.TRUE.toString());
        config.addDataSourceProperty("cacheResultSetMetadata", Boolean.TRUE.toString());
        config.addDataSourceProperty("cacheServerConfiguration", Boolean.TRUE.toString());
        config.addDataSourceProperty("elideSetAutoCommits", Boolean.TRUE.toString());
        config.addDataSourceProperty("maintainTimeStats", Boolean.FALSE.toString());
        config.addDataSourceProperty("netTimeoutForStreamingResults", 0);
        return new HikariDataSource(config);
    }
    
    private static void validateDriverClassName(final String driverClassName) {
        try {
            Class.forName(driverClassName);
        } catch (final ClassNotFoundException ex) {
            throw new ShardingException("Cannot load JDBC driver class `%s`, make sure it in classpath.", driverClassName);
        }
    }
}
