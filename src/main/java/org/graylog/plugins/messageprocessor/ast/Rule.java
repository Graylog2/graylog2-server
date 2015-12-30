package org.graylog.plugins.messageprocessor.ast;

import com.google.auto.value.AutoValue;
import org.graylog.plugins.messageprocessor.ast.expressions.LogicalExpression;
import org.graylog.plugins.messageprocessor.ast.statements.Statement;

import java.util.Collection;

@AutoValue
public abstract class Rule {

    public abstract String name();

    public abstract int stage();

    public abstract LogicalExpression when();

    public abstract Collection<Statement> then();

    public static Builder builder() {
        return new AutoValue_Rule.Builder().stage(0);
    }

    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder name(String name);
        public abstract Builder stage(int stage);
        public abstract Builder when(LogicalExpression condition);
        public abstract Builder then(Collection<Statement> actions);

        public abstract Rule build();
    }

}
