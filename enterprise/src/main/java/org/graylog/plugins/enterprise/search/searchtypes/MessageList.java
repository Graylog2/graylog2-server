package org.graylog.plugins.enterprise.search.searchtypes;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.enterprise.search.SearchType;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonTypeName(MessageList.NAME)
@JsonDeserialize(builder = MessageList.Builder.class)
public abstract class MessageList implements SearchType {
    public static final String NAME = "messages";

    @Override
    public abstract String type();

    public abstract int limit();

    public abstract int offset();

    @Nullable
    public abstract List<Sort> sort();

    public static Builder builder() {
        return new AutoValue_MessageList.Builder()
                .type(NAME)
                .limit(150)
                .offset(0);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder type(String type);

        public abstract Builder limit(int limit);

        public abstract Builder offset(int offset);

        public abstract Builder sort(@Nullable List<Sort> sort);

        public abstract MessageList build();
    }
}
