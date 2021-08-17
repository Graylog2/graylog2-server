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
package org.graylog.storage.elasticsearch6.indices;

import io.searchbox.action.AbstractMultiIndexActionBuilder;
import io.searchbox.action.GenericResultAbstractAction;

public class GetSingleAlias extends GenericResultAbstractAction {
    private final String alias;

    protected GetSingleAlias(Builder builder) {
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

    public static class Builder extends AbstractMultiIndexActionBuilder<GetSingleAlias, Builder> {
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
