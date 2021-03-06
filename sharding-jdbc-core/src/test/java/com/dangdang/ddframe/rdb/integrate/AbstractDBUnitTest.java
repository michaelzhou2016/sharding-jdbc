/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.integrate;

import com.dangdang.ddframe.rdb.integrate.sql.DatabaseTestSQL;
import com.dangdang.ddframe.rdb.integrate.sql.mysql.MySQLTestSQL;
import com.dangdang.ddframe.rdb.integrate.sql.oracle.OracleSQLTestSQL;
import com.dangdang.ddframe.rdb.integrate.sql.postgresql.PostgreSQLTestSQL;
import com.dangdang.ddframe.rdb.integrate.sql.sqlserver.SQLServerSQLTestSQL;
import com.dangdang.ddframe.rdb.sharding.constant.DatabaseType;
import org.apache.commons.dbcp.BasicDataSource;
import org.dbunit.DatabaseUnitException;
import org.dbunit.IDatabaseTester;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.ext.h2.H2Connection;
import org.dbunit.ext.mssql.MsSqlConnection;
import org.dbunit.ext.mysql.MySqlConnection;
import org.dbunit.ext.oracle.OracleConnection;
import org.dbunit.operation.DatabaseOperation;
import org.h2.tools.RunScript;
import org.junit.Before;

import javax.sql.DataSource;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dangdang.ddframe.rdb.sharding.constant.DatabaseType.*;
import static org.dbunit.Assertion.assertEquals;

public abstract class AbstractDBUnitTest {
    
    protected static final DatabaseType CURRENT_DB_TYPE = H2;
    
    protected static DatabaseTestSQL sql;
    
    private static final Map<String, DataSource> DATA_SOURCES = new HashMap<>();
    
    private final DataBaseEnvironment dbEnv = new DataBaseEnvironment(CURRENT_DB_TYPE);
    
    @Before
    public void createSql() {
        sql = currentDatabaseTestSQL();
    }
    
    @Before
    public void createSchema() throws SQLException {
        for (String each : getSchemaFiles()) {
            Connection conn = createDataSource(each).getConnection();
            RunScript.execute(conn, new InputStreamReader(AbstractDBUnitTest.class.getClassLoader().getResourceAsStream(each)));
            conn.close();
        }
    }
    
    @Before
    public final void importDataSet() throws Exception {
        for (String each : getDataSetFiles()) {
            InputStream is = AbstractDBUnitTest.class.getClassLoader().getResourceAsStream(each);
            IDataSet dataSet = new FlatXmlDataSetBuilder().build(new InputStreamReader(is));
            IDatabaseTester databaseTester = new ShardingJdbcDatabaseTester(dbEnv.getDriverClassName(), dbEnv.getURL(getFileName(each)), dbEnv.getUsername(), dbEnv.getPassword(), dbEnv.getSchema());
            databaseTester.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
            databaseTester.setDataSet(dataSet);
            databaseTester.onSetup();
        }
    }
    
    protected abstract List<String> getSchemaFiles();
    
    protected abstract List<String> getDataSetFiles();
    
    protected final String currentDbType() {
        return H2 == CURRENT_DB_TYPE ? "mysql" : CURRENT_DB_TYPE.name().toLowerCase();
    }
    
    private DatabaseTestSQL currentDatabaseTestSQL() {
        switch (dbEnv.getDatabaseType()) {
            case H2:
            case MySQL:
                return new MySQLTestSQL();
            case PostgreSQL:
                return new PostgreSQLTestSQL();
            case Oracle:
                return new OracleSQLTestSQL();
            case SQLServer:
                return new SQLServerSQLTestSQL();
            default:
                throw new UnsupportedOperationException(dbEnv.getDatabaseType().name());
        }
    }
    
    protected final boolean isAliasSupport() {
        return H2.equals(dbEnv.getDatabaseType()) || MySQL.equals(dbEnv.getDatabaseType());
    }
    
    protected final Map<String, DataSource> createDataSourceMap(final String dataSourceNamePattern) {
        Map<String, DataSource> result = new HashMap<>(getDataSetFiles().size());
        for (String each : getDataSetFiles()) {
            result.put(String.format(dataSourceNamePattern, getFileName(each)), createDataSource(each));
        }
        return result;
    }
    
    private DataSource createDataSource(final String dataSetFile) {
        if (DATA_SOURCES.containsKey(dataSetFile)) {
            return DATA_SOURCES.get(dataSetFile);
        }
        BasicDataSource result = new BasicDataSource();
        result.setDriverClassName(dbEnv.getDriverClassName());
        result.setUrl(dbEnv.getURL(getFileName(dataSetFile)));
        result.setUsername(dbEnv.getUsername());
        result.setPassword(dbEnv.getPassword());
        result.setMaxActive(1000);
        DATA_SOURCES.put(dataSetFile, result);
        return result;
    }
    
    private String getFileName(final String dataSetFile) {
        String fileName = new File(dataSetFile).getName();
        if (-1 == fileName.lastIndexOf(".")) {
            return fileName;
        }
        return fileName.substring(0, fileName.lastIndexOf("."));
    }
    
    protected void assertDataSet(final String expectedDataSetFile, final Connection connection, final String actualTableName, final String sql, final Object... params) 
            throws SQLException, DatabaseUnitException {
        try (
                Connection conn = connection;
                PreparedStatement ps = conn.prepareStatement(sql)) {
            int i = 1;
            for (Object each : params) {
                ps.setObject(i++, each);
            }
            ITable actualTable = getITable(connection, ps, actualTableName, sql);
            IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(new InputStreamReader(AbstractDBUnitTest.class.getClassLoader().getResourceAsStream(expectedDataSetFile)));
            assertEquals(expectedDataSet.getTable(actualTableName), actualTable);
        }
    }
    
    protected void assertDataSet(final String expectedDataSetFile, final Connection connection, final String actualTableName, final String sql) throws SQLException, DatabaseUnitException {
        try (Connection conn = connection) {
            ITable actualTable = getConnection(conn).createQueryTable(actualTableName, sql);
            IDataSet expectedDataSet = new FlatXmlDataSetBuilder().build(new InputStreamReader(AbstractDBUnitTest.class.getClassLoader().getResourceAsStream(expectedDataSetFile)));
            assertEquals(expectedDataSet.getTable(actualTableName), actualTable);
        }
    }
    
    private ITable getITable(final Connection connection, final PreparedStatement preparedStatement, final String tableName, final String sql) throws SQLException, DatabaseUnitException {
        return getConnection(connection).createTable(tableName, preparedStatement);
    }
    
    private IDatabaseConnection getConnection(final Connection connection) throws DatabaseUnitException {
        switch (dbEnv.getDatabaseType()) {
            case H2: 
                return new H2Connection(connection, "PUBLIC");
            case MySQL: 
                return new MySqlConnection(connection, null);
            case PostgreSQL:
                return new DatabaseConnection(connection);
            case Oracle:
                return new OracleConnection(connection, "JDBC");
            case SQLServer:
                return new MsSqlConnection(connection);
            default: 
                throw new UnsupportedOperationException(dbEnv.getDatabaseType().name());
        }
    }
}
