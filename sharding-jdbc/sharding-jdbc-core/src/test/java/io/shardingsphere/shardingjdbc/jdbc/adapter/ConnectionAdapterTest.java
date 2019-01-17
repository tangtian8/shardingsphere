/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.shardingsphere.shardingjdbc.jdbc.adapter;

import com.google.common.collect.Multimap;
import io.shardingsphere.shardingjdbc.common.base.AbstractShardingJDBCDatabaseAndTableTest;
import io.shardingsphere.shardingjdbc.jdbc.core.connection.ShardingConnection;
import io.shardingsphere.shardingjdbc.jdbc.core.fixture.BASEShardingTransactionManagerFixture;
import io.shardingsphere.shardingjdbc.jdbc.core.fixture.XAShardingTransactionManagerFixture;
import io.shardingsphere.transaction.core.TransactionOperationType;
import io.shardingsphere.transaction.core.TransactionType;
import io.shardingsphere.transaction.core.TransactionTypeHolder;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public final class ConnectionAdapterTest extends AbstractShardingJDBCDatabaseAndTableTest {
    
    private final String sql = "SELECT 1";
    
    @After
    public void tearDown() {
        TransactionTypeHolder.clear();
        XAShardingTransactionManagerFixture.getInvocations().clear();
        BASEShardingTransactionManagerFixture.getInvocations().clear();
    }
    
    @Test
    public void assertSetAutoCommit() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            assertTrue(actual.getAutoCommit());
            actual.setAutoCommit(false);
            actual.createStatement().executeQuery(sql);
            assertFalse(actual.getAutoCommit());
            Multimap<String, Connection> cachedConnections = getCachedConnections(actual);
            assertThat(cachedConnections.size(), is(1));
            for (Connection each : cachedConnections.values()) {
                assertFalse(each.getAutoCommit());
            }
        }
    }
    
    @Test
    public void assertIgnoreAutoCommitForXA() throws SQLException {
        TransactionTypeHolder.set(TransactionType.XA);
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.setAutoCommit(true);
            assertFalse(XAShardingTransactionManagerFixture.getInvocations().contains(TransactionOperationType.BEGIN));
        }
    }
    
    @Test
    public void assertIgnoreAutoCommitForBASE() throws SQLException {
        TransactionTypeHolder.set(TransactionType.BASE);
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.setAutoCommit(true);
            assertFalse(BASEShardingTransactionManagerFixture.getInvocations().contains(TransactionOperationType.BEGIN));
        }
    }
    
    @Test
    public void assertCommit() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.setAutoCommit(false);
            actual.createStatement().executeQuery(sql);
            actual.commit();
        }
    }
    
    @Test
    public void assertXACommit() throws SQLException {
        TransactionTypeHolder.set(TransactionType.XA);
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.commit();
            assertTrue(XAShardingTransactionManagerFixture.getInvocations().contains(TransactionOperationType.COMMIT));
        }
    }
    
    @Test
    public void assertRollback() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.setAutoCommit(false);
            actual.createStatement().executeQuery(sql);
            actual.rollback();
        }
    }
    
    @Test
    public void assertXARollback() throws SQLException {
        TransactionTypeHolder.set(TransactionType.XA);
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.rollback();
            assertTrue(XAShardingTransactionManagerFixture.getInvocations().contains(TransactionOperationType.ROLLBACK));
        }
    }
    
    @Test
    public void assertClose() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.createStatement().executeQuery(sql);
            actual.close();
            assertClose(actual);
        }
    }
    
    private void assertClose(final ShardingConnection actual) {
        assertTrue(actual.isClosed());
        Multimap<String, Connection> cachedConnections = getCachedConnections(actual);
        assertTrue(cachedConnections.isEmpty());
    }
    
    @Test
    public void assertSetReadOnly() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            assertTrue(actual.isReadOnly());
            actual.setReadOnly(false);
            actual.createStatement().executeQuery(sql);
            assertReadOnly(actual, false);
            actual.setReadOnly(true);
            assertReadOnly(actual, true);
        }
    }
    
    private void assertReadOnly(final ShardingConnection actual, final boolean readOnly) throws SQLException {
        assertThat(actual.isReadOnly(), is(readOnly));
        Multimap<String, Connection> cachedConnections = getCachedConnections(actual);
        assertThat(cachedConnections.size(), is(1));
        for (Connection each : cachedConnections.values()) {
            assertThat(each.isReadOnly(), is(readOnly));
        }
    }
    
    @Test
    public void assertGetTransactionIsolation() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.createStatement().executeQuery(sql);
            assertThat(actual.getTransactionIsolation(), is(Connection.TRANSACTION_READ_COMMITTED));
        }
    }
    
    @Test
    public void assertSetTransactionIsolation() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            assertThat(actual.getTransactionIsolation(), is(Connection.TRANSACTION_READ_UNCOMMITTED));
            actual.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            actual.createStatement().executeQuery(sql);
            assertTransactionIsolation(actual, Connection.TRANSACTION_SERIALIZABLE);
            actual.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            assertTransactionIsolation(actual, Connection.TRANSACTION_READ_COMMITTED);
        }
    }
    
    private void assertTransactionIsolation(final ShardingConnection actual, final int transactionIsolation) throws SQLException {
        assertThat(actual.getTransactionIsolation(), is(transactionIsolation));
        Multimap<String, Connection> cachedConnections = getCachedConnections(actual);
        assertThat(cachedConnections.size(), is(1));
        for (Connection each : cachedConnections.values()) {
            assertThat(each.getTransactionIsolation(), is(transactionIsolation));
        }
    }
    
    @Test
    public void assertGetWarnings() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            assertNull(actual.getWarnings());
        }
    }
    
    @Test
    public void assertClearWarnings() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.clearWarnings();
        }
    }
    
    @Test
    public void assertGetHoldability() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            assertThat(actual.getHoldability(), is(ResultSet.CLOSE_CURSORS_AT_COMMIT));
        }
    }
    
    @Test
    public void assertSetHoldability() throws SQLException {
        try (ShardingConnection actual = getShardingDataSource().getConnection()) {
            actual.setHoldability(ResultSet.CONCUR_READ_ONLY);
            assertThat(actual.getHoldability(), is(ResultSet.CLOSE_CURSORS_AT_COMMIT));
        }
    }
    
    @SuppressWarnings("unchecked")
    @SneakyThrows
    private Multimap<String, Connection> getCachedConnections(final AbstractConnectionAdapter connectionAdapter) {
        Field field = AbstractConnectionAdapter.class.getDeclaredField("cachedConnections");
        field.setAccessible(true);
        return (Multimap<String, Connection>) field.get(connectionAdapter);
    }
}
