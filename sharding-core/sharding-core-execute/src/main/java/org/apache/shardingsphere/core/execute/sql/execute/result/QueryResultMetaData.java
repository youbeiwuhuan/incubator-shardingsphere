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

package org.apache.shardingsphere.core.execute.sql.execute.result;

import com.google.common.base.Optional;
import lombok.SneakyThrows;
import org.apache.shardingsphere.core.rule.EncryptRule;
import org.apache.shardingsphere.core.rule.ShardingRule;
import org.apache.shardingsphere.core.rule.TableRule;
import org.apache.shardingsphere.spi.encrypt.ShardingEncryptor;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Query result meta data.
 *
 * @author panjuan
 * @author yangyi
 */
public final class QueryResultMetaData {
    
    private final ResultSetMetaData resultSetMetaData;
    
    private final ShardingRule shardingRule;
    
    private final EncryptRule encryptRule;
    
    private final Map<String, Integer> columnLabelAndIndexes;
    
    @SneakyThrows
    public QueryResultMetaData(final ResultSetMetaData resultSetMetaData, final ShardingRule shardingRule) {
        this.resultSetMetaData = resultSetMetaData;
        this.shardingRule = shardingRule;
        this.encryptRule = shardingRule.getEncryptRule();
        columnLabelAndIndexes = getColumnLabelAndIndexMap();
    }
    
    @SneakyThrows
    public QueryResultMetaData(final ResultSetMetaData resultSetMetaData, final EncryptRule encryptRule) {
        this.resultSetMetaData = resultSetMetaData;
        this.shardingRule = null;
        this.encryptRule = encryptRule;
        columnLabelAndIndexes = getColumnLabelAndIndexMap();
    }
    
    @SneakyThrows
    public QueryResultMetaData(final ResultSetMetaData resultSetMetaData) {
        this.resultSetMetaData = resultSetMetaData;
        this.shardingRule = null;
        this.encryptRule = new EncryptRule();
        columnLabelAndIndexes = getColumnLabelAndIndexMap();
    }
    
    @SneakyThrows
    private Map<String, Integer> getColumnLabelAndIndexMap() {
        Map<String, Integer> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (int columnIndex = resultSetMetaData.getColumnCount(); columnIndex > 0; columnIndex--) {
            result.put(resultSetMetaData.getColumnLabel(columnIndex), columnIndex);
        }
        return result;
    }
    
    /**
     * Get column count.
     * 
     * @return column count
     */
    @SneakyThrows
    public int getColumnCount() {
        return resultSetMetaData.getColumnCount();
    }
    
    /**
     * Get column label.
     * 
     * @param columnIndex column index
     * @return column label
     */
    @SneakyThrows
    public String getColumnLabel(final int columnIndex) {
        return resultSetMetaData.getColumnLabel(columnIndex);
    }
    
    /**
     * Get column name.
     * 
     * @param columnIndex column index
     * @return column name
     */
    @SneakyThrows
    public String getColumnName(final int columnIndex) {
        return resultSetMetaData.getColumnName(columnIndex);
    }
    
    /**
     * Get column index.
     * 
     * @param columnLabel column label
     * @return column name
     */
    public Integer getColumnIndex(final String columnLabel) {
        return columnLabelAndIndexes.get(columnLabel);
    }
    
    /**
     * Whether the column value is case sensitive.
     *
     * @param columnIndex column index
     * @return true if column is case sensitive, otherwise false
     */
    @SneakyThrows
    public boolean isCaseSensitive(final int columnIndex) {
        return resultSetMetaData.isCaseSensitive(columnIndex);
    }
    
    /**
     * Get sharding encryptor.
     * 
     * @param columnIndex column index
     * @return sharding encryptor optional
     */
    @SneakyThrows
    public Optional<ShardingEncryptor> getShardingEncryptor(final int columnIndex) {
        String logicTable = getTableName(columnIndex);
        return encryptRule.getShardingEncryptor(logicTable, getLogicColumn(logicTable, columnIndex));
    }
    
    private String getTableName(final int columnIndex) throws SQLException {
        String actualTableName = resultSetMetaData.getTableName(columnIndex);
        if (null == shardingRule) {
            return actualTableName;
        }
        Optional<TableRule> tableRule = shardingRule.findTableRuleByActualTable(actualTableName);
        return tableRule.isPresent() ? tableRule.get().getLogicTable() : actualTableName;
    }
    
    @SneakyThrows
    private String getLogicColumn(final String tableName, final int columnIndex) {
        String columnLabel = resultSetMetaData.getColumnName(columnIndex);
        return encryptRule.isCipherColumn(tableName, columnLabel) ? encryptRule.getLogicColumn(tableName, columnLabel) : columnLabel;
    }
}
