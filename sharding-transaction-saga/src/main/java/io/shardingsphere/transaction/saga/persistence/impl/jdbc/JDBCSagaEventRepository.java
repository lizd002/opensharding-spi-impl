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

package io.shardingsphere.transaction.saga.persistence.impl.jdbc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.servicecomb.saga.core.JacksonToJsonFormat;
import org.apache.servicecomb.saga.core.SagaEvent;
import org.apache.servicecomb.saga.core.ToJsonFormat;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * JDBC saga event repository.
 *
 * @author yangyi
 */
@RequiredArgsConstructor
@Slf4j
public final class JDBCSagaEventRepository {
    
    private static final String INSERT_SQL = "INSERT INTO saga_event (saga_id, type, content_json) values (?, ?, ?)";
    
    private static final String FIND_INCOMPLETE_SAGA_EVENTS_SQL = "SELECT * FROM saga_event WHERE saga_id NOT IN (SELECT DISTINCT saga_id FROM saga_event WHERE type = 'SagaEndedEvent')";
    
    private final DataSource dataSource;
    
    private final ToJsonFormat toJsonFormat = new JacksonToJsonFormat();
    
    /**
     * Insert new saga event.
     *
     * @param sagaEvent saga event
     */
    public void insert(final SagaEvent sagaEvent) {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setObject(1, sagaEvent.sagaId);
            statement.setObject(2, sagaEvent.getClass().getSimpleName());
            statement.setObject(3, sagaEvent.json(toJsonFormat));
            statement.executeUpdate();
        } catch (SQLException ex) {
            log.warn("Persist saga event failed", ex);
        }
    }
}
