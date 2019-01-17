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

package io.shardingsphere.core.parsing.parser.dialect.mysql.sql;

import io.shardingsphere.core.metadata.table.ShardingTableMetaData;
import io.shardingsphere.core.parsing.lexer.LexerEngine;
import io.shardingsphere.core.parsing.parser.dialect.mysql.clause.facade.MySQLInsertClauseParserFacade;
import io.shardingsphere.core.parsing.parser.sql.dml.insert.AbstractInsertParser;
import io.shardingsphere.core.rule.ShardingRule;

/**
 * Insert parser for MySQL.
 *
 * @author zhangliang
 * @author panjuan
 */
public final class MySQLInsertParser extends AbstractInsertParser {
    
    public MySQLInsertParser(final ShardingRule shardingRule, final LexerEngine lexerEngine, final ShardingTableMetaData shardingTableMetaData) {
        super(shardingRule, shardingTableMetaData, lexerEngine, new MySQLInsertClauseParserFacade(shardingRule, lexerEngine));
    }
}
