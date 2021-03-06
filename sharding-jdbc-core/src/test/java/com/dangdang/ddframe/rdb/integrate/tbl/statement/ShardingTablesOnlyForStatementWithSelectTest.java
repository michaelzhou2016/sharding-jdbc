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

package com.dangdang.ddframe.rdb.integrate.tbl.statement;

import com.dangdang.ddframe.rdb.integrate.tbl.AbstractShardingTablesOnlyDBUnitTest;
import com.dangdang.ddframe.rdb.sharding.jdbc.core.datasource.ShardingDataSource;
import org.dbunit.DatabaseUnitException;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;

import static com.dangdang.ddframe.rdb.sharding.constant.DatabaseType.Oracle;
import static com.dangdang.ddframe.rdb.sharding.constant.DatabaseType.PostgreSQL;
import static com.dangdang.ddframe.rdb.sharding.constant.DatabaseType.SQLServer;

public final class ShardingTablesOnlyForStatementWithSelectTest extends AbstractShardingTablesOnlyDBUnitTest {
    
    private ShardingDataSource shardingDataSource;
    
    @Before
    public void init() throws SQLException {
        shardingDataSource = getShardingDataSource();
    }
    
    @Test
    public void assertSelectEqualsWithSingleTable() throws SQLException, DatabaseUnitException {
        assertDataSet("integrate/dataset/tbl/expect/select/SelectEqualsWithSingleTable_0.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectEqualsWithSingleTableSql(), 10, 1000));
        assertDataSet("integrate/dataset/tbl/expect/select/SelectEqualsWithSingleTable_1.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectEqualsWithSingleTableSql(), 11, 1109));
        assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(), "t_order", String.format(sql.getSelectEqualsWithSingleTableSql(), 12, 1000));
    }
    
    @Test
    public void assertSelectBetweenWithSingleTable() throws SQLException, DatabaseUnitException {
        assertDataSet("integrate/dataset/tbl/expect/select/SelectBetweenWithSingleTable.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectBetweenWithSingleTableSql(), 10, 12, 1009, 1108));
        assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectBetweenWithSingleTableSql(), 10, 12, 1309, 1408));
    }
    
    @Test
    public void assertSelectInWithSingleTable() throws SQLException, DatabaseUnitException {
        assertDataSet("integrate/dataset/tbl/expect/select/SelectInWithSingleTable_0.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectInWithSingleTableSql(), 10, 11, 15, 1009, 1108));
        assertDataSet("integrate/dataset/tbl/expect/select/SelectInWithSingleTable_1.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectInWithSingleTableSql(), 10, 12, 15, 1009, 1108));
        assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(), 
                "t_order", String.format(sql.getSelectInWithSingleTableSql(), 10, 12, 15, 1309, 1408));
    }
    
    @Test
    public void assertSelectLimitWithBindingTable() throws SQLException, DatabaseUnitException {
        if (!Oracle.name().equalsIgnoreCase(currentDbType()) && !SQLServer.name().equalsIgnoreCase(currentDbType())) {
            String expectedDataSetFile = PostgreSQL.name().equalsIgnoreCase(currentDbType()) ? "integrate/dataset/tbl/expect/select/postgresql/SelectLimitWithBindingTable.xml"
                    : "integrate/dataset/tbl/expect/select/SelectLimitWithBindingTable.xml";
            assertDataSet(expectedDataSetFile, shardingDataSource.getConnection(),
                    "t_order_item", String.format(sql.getSelectLimitWithBindingTableSql(), 10, 19, 1000, 1909, 2, 2));
            assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(),
                    "t_order_item", String.format(sql.getSelectLimitWithBindingTableSql(), 10, 19, 1000, 1909, 10000, 2));
        }
    }
    
    @Test
    public void assertSelectGroupByWithBindingTable() throws SQLException, DatabaseUnitException {
        assertDataSet("integrate/dataset/tbl/expect/select/SelectGroupByWithBindingTable.xml", shardingDataSource.getConnection(), 
                "t_order_item", String.format(sql.getSelectGroupWithBindingTableSql(), 10, 11, 1000, 1109));
        assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(), 
                "t_order_item", String.format(sql.getSelectGroupWithBindingTableSql(), 1, 9, 1000, 1909));
    }
    
    @Test
    public void assertSelectGroupByWithoutGroupedColumn() throws SQLException, DatabaseUnitException {
        String expectedDataSetFile = PostgreSQL.name().equalsIgnoreCase(currentDbType()) || Oracle.name().equalsIgnoreCase(currentDbType()) || SQLServer.name().equalsIgnoreCase(currentDbType())
                ? "integrate/dataset/tbl/expect/select/postgresql/SelectGroupByWithoutGroupedColumn.xml"
                : "integrate/dataset/tbl/expect/select/SelectGroupByWithoutGroupedColumn.xml";
        assertDataSet(expectedDataSetFile, shardingDataSource.getConnection(),
                "t_order_item", String.format(sql.getSelectGroupWithoutGroupedColumnSql(), 10, 11, 1000, 1109));
        assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(),
                "t_order_item", String.format(sql.getSelectGroupWithoutGroupedColumnSql(), 1, 9, 1000, 1909));
    }
    
    @Test
    public void assertSelectWithBindingTableAndConfigTable() throws SQLException, DatabaseUnitException {
        String expectedDataSetFile = PostgreSQL.name().equalsIgnoreCase(currentDbType()) || Oracle.name().equalsIgnoreCase(currentDbType()) || SQLServer.name().equalsIgnoreCase(currentDbType())
                ? "integrate/dataset/tbl/expect/select/postgresql/SelectWithBindingTableAndConfigTable.xml"
                : "integrate/dataset/tbl/expect/select/SelectWithBindingTableAndConfigTable.xml";
        assertDataSet(expectedDataSetFile, shardingDataSource.getConnection(),
                "t_order_item", String.format(sql.getSelectGroupWithBindingTableAndConfigSql(), 10, 11, 1009, 1108, "'init'"));
        assertDataSet("integrate/dataset/Empty.xml", shardingDataSource.getConnection(),
                "t_order_item", String.format(sql.getSelectGroupWithBindingTableAndConfigSql(), 10, 11, 1009, 1108, "'none'"));
    }
}
