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
