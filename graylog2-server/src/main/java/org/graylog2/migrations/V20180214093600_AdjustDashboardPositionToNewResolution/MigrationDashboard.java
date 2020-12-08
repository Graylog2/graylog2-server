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
package org.graylog2.migrations.V20180214093600_AdjustDashboardPositionToNewResolution;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Integer.parseInt;

class MigrationDashboard {
    static final String FIELD_ID = "_id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CONTENT_PACK = "content_pack";
    private static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    private static final String FIELD_CREATED_AT = "created_at";
    private static final String EMBEDDED_POSITIONS = "positions";

    private final Map<String, Object> fields;
    private final ObjectId id;
    private final AtomicReference<String> hexId = new AtomicReference<>(null);

    MigrationDashboard(final ObjectId id, @Nullable final Map<String, Object> fields) {
        this.id = id;

        if (null != this.id) {
            hexId.set(this.id.toHexString());
        }

        if(fields == null) {
            this.fields = new HashMap<>();
        } else {
            this.fields = new HashMap<>(fields.size());

            // Transform all java.util.Date's to JodaTime because MongoDB gives back java.util.Date's. #lol
            for (Map.Entry<String, Object> field : fields.entrySet()) {
                final String key = field.getKey();
                final Object value = field.getValue();
                if (value instanceof Date) {
                    this.fields.put(key, new DateTime(value, DateTimeZone.UTC));
                } else {
                    this.fields.put(key, value);
                }
            }
        }
    }

    String getTitle() {
        return (String) fields.get(FIELD_TITLE);
    }

    List<WidgetPosition> getPositions() {
        final BasicDBObject positions = (BasicDBObject) fields.get(MigrationDashboard.EMBEDDED_POSITIONS);
        if (positions == null) {
            return Collections.emptyList();
        }

        final List<WidgetPosition> result = new ArrayList<>(positions.size());
        for ( String positionId : positions.keySet() ) {
            final BasicDBObject position = (BasicDBObject) positions.get(positionId);
            final int width = parseInt(position.getString("width", "1"));
            final int height = parseInt(position.getString("height", "1"));
            final int col = parseInt(position.getString("col", "1"));
            final int row = parseInt(position.getString("row","1"));
            final WidgetPosition widgetPosition = WidgetPosition.builder()
                    .id(positionId)
                    .width(width)
                    .height(height)
                    .col(col)
                    .row(row)
                    .build();
            result.add(widgetPosition);
        }
        return result;
    }

    void setPositions(List<WidgetPosition> widgetPositions) {
        checkNotNull(widgetPositions, "widgetPositions must be given");
        final Map<String, Map<String, Integer>> positions = new HashMap<>(widgetPositions.size());
        for (WidgetPosition widgetPosition : widgetPositions) {
            Map<String, Integer> position = new HashMap<>(4);
            position.put("width", widgetPosition.width());
            position.put("height", widgetPosition.height());
            position.put("col", widgetPosition.col());
            position.put("row", widgetPosition.row());
            positions.put(widgetPosition.id(), position);
        }
        Map<String, Object> fields = getFields();
        checkNotNull(fields, "No fields found!");
        fields.put(MigrationDashboard.EMBEDDED_POSITIONS, positions);
    }

    Map<String, Object> getFields() {
        return fields;
    }

    Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>builder()
                .put(FIELD_TITLE, new FilledStringValidator())
                .put(FIELD_DESCRIPTION, new FilledStringValidator())
                .put(FIELD_CONTENT_PACK, new OptionalStringValidator())
                .put(FIELD_CREATOR_USER_ID, new FilledStringValidator())
                .put(FIELD_CREATED_AT, new DateValidator())
                .build();
    }

    String getId() {
        // Performance - toHexString is expensive so we cache it.
        final String s = hexId.get();
        if (s == null && id != null) {
            final String hexString = getObjectId().toHexString();
            hexId.compareAndSet(null, hexString);
            return hexString;
        }

        return s;
    }

    private ObjectId getObjectId() {
        return this.id;
    }
}
