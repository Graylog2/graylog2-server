package org.graylog.storage.elasticsearch6.indices;

import io.searchbox.action.AbstractMultiIndexActionBuilder;
import io.searchbox.action.GenericResultAbstractAction;

public class GetSingleAlias extends GenericResultAbstractAction {
    private final String alias;

    protected GetSingleAlias(GetSingleAlias.Builder builder) {
        super(builder);
        this.alias = builder.alias;
        setURI(buildURI());
    }

    @Override
    public String getRestMethodName() {
        return "GET";
    }

    @Override
    protected String buildURI() {
        return super.buildURI() + "/_alias/" + this.alias;
    }

    public static class Builder extends AbstractMultiIndexActionBuilder<GetSingleAlias, GetSingleAlias.Builder> {
        private String alias;

        public Builder alias(String alias) {
            this.alias = alias;
            return this;
        }

        @Override
        public GetSingleAlias build() {
            return new GetSingleAlias(this);
        }
    }
}
