/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.alarmcallbacks;

import com.google.common.collect.Lists;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.graylog2.alerts.Alert;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackError;
import org.graylog2.rest.models.alarmcallbacks.AlarmCallbackSuccess;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.List;

public class AlarmCallbackHistoryServiceImpl implements AlarmCallbackHistoryService {
    private final JacksonDBCollection<AlarmCallbackHistoryImpl, String> coll;

    @Inject
    public AlarmCallbackHistoryServiceImpl(MongoConnection mongoConnection,
                                           MongoJackObjectMapperProvider mapperProvider) {
        final String collectionName = AlarmCallbackHistoryImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, AlarmCallbackHistoryImpl.class, String.class, mapperProvider.get());
        dbCollection.createIndex(AlarmCallbackHistoryImpl.FIELD_ALERTID);
    }

    @Override
    public List<AlarmCallbackHistory> getForAlertId(String alertId) {
        return toAbstractListType(coll.find(DBQuery.is(AlarmCallbackHistoryImpl.FIELD_ALERTID, alertId)).toArray());
    }

    @Override
    public AlarmCallbackHistory success(AlarmCallbackConfiguration alarmCallbackConfiguration, Alert alert, AlertCondition alertCondition) {
        return AlarmCallbackHistoryImpl.create(new ObjectId().toHexString(), alarmCallbackConfiguration, alert, alertCondition, AlarmCallbackSuccess.create());
    }

    @Override
    public AlarmCallbackHistory error(AlarmCallbackConfiguration alarmCallbackConfiguration, Alert alert, AlertCondition alertCondition, String error) {
        return AlarmCallbackHistoryImpl.create(new ObjectId().toHexString(), alarmCallbackConfiguration, alert, alertCondition, AlarmCallbackError.create(error));
    }

    @Override
    public AlarmCallbackHistory save(AlarmCallbackHistory alarmCallbackHistory) {
        final AlarmCallbackHistoryImpl historyImpl = implOrFail(alarmCallbackHistory);
        final WriteResult<AlarmCallbackHistoryImpl, String> writeResult = coll.save(historyImpl);
        return writeResult.getSavedObject();
    }

    private List<AlarmCallbackHistory> toAbstractListType(List<AlarmCallbackHistoryImpl> histories) {
        final List<AlarmCallbackHistory> result = Lists.newArrayListWithCapacity(histories.size());
        result.addAll(histories);

        return result;
    }

    private AlarmCallbackHistoryImpl implOrFail(AlarmCallbackHistory history) {
        final AlarmCallbackHistoryImpl historyImpl;
        if (history instanceof AlarmCallbackHistoryImpl) {
            historyImpl = (AlarmCallbackHistoryImpl) history;
            return historyImpl;
        } else {
            throw new IllegalArgumentException("Supplied output must be of implementation type AlarmCallbackHistoryImpl, not " + history.getClass());
        }
    }
}
