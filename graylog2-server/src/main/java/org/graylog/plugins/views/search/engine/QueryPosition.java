package org.graylog.plugins.views.search.engine;

import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.validation.ImmutableToken;
import org.graylog.plugins.views.search.validation.SubstringMultilinePosition;

@AutoValue
public abstract class QueryPosition {

    public abstract int beginLine();

    public abstract int beginColumn();

    public abstract int endLine();

    public abstract int endColumn();

    public static Builder builder() {
        return new AutoValue_QueryPosition.Builder();
    }

    public static QueryPosition create(int beginLine, int beginColumn, int endLine, int endColumn) {
        return builder()
                .beginLine(beginLine)
                .beginColumn(beginColumn)
                .endLine(endLine)
                .endColumn(endColumn)
                .build();
    }


    public static QueryPosition from(SubstringMultilinePosition pos) {
        return builder()
                .beginLine(pos.line())
                .beginColumn(pos.beginColumn())
                .endLine(pos.line())
                .endColumn(pos.endColumn())
                .build();
    }

    public static QueryPosition from(ImmutableToken token) {
        return builder()
                .beginLine(token.beginLine())
                .beginColumn(token.beginColumn())
                .endLine(token.endLine())
                .endColumn(token.endColumn())
                .build();
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder beginLine(int line);

        public abstract Builder beginColumn(int column);

        public abstract Builder endLine(int line);

        public abstract Builder endColumn(int column);

        public abstract QueryPosition build();
    }
}
