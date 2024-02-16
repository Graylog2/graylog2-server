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
package org.graylog2.indexer.datastream.policy.actions;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface WrappedAction {

    // actions need to be registered here as @JsonTypeInfo cannot be used due to the sibling "retry" node.
    // enum name must match action name as defined in
    // https://opensearch.org/docs/latest/im-plugin/ism/policies/#ism-supported-operations
    enum Type {
        DELETE(DeleteAction.class),
        ROLLOVER(RolloverAction.class),
        ROLLUP(RollupAction.class);

        public final Class<? extends WrappedAction> implementingClass;

        Type(Class<? extends WrappedAction> implementingClass) {
            this.implementingClass = implementingClass;
        }
    }

    @JsonIgnore
    Type getType();

}
