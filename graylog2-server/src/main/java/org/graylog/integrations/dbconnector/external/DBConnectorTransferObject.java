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
package org.graylog.integrations.dbconnector.external;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

@AutoValue
public abstract class DBConnectorTransferObject {
    public abstract String databaseType();

    @Nullable
    public abstract String stateFieldType();

    @Nullable
    public abstract Object stateFieldValue();

    @Nullable
    public abstract String stateField();

    @Nullable
    public abstract String tableName();

    @Nullable
    public abstract String databaseName();

    @Nullable
    public abstract String mongoCollectionName();

    public static Builder builder() {
        return new AutoValue_DBConnectorTransferObject.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder stateFieldType(String stateFieldType);

        public abstract Builder stateFieldValue(Object stateFieldType);

        public abstract Builder stateField(String stateFieldType);

        public abstract Builder tableName(String stateFieldType);

        public abstract Builder databaseName(String stateFieldType);

        public abstract Builder mongoCollectionName(String stateFieldType);

        public abstract Builder databaseType(String databaseType);

        public abstract DBConnectorTransferObject build();
    }
}
