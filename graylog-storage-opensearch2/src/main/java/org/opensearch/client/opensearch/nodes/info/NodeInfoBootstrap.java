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
/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.client.opensearch.nodes.info;

import jakarta.json.stream.JsonGenerator;

import java.util.function.Function;

import org.opensearch.client.json.JsonpDeserializable;
import org.opensearch.client.json.JsonpDeserializer;
import org.opensearch.client.json.JsonpMapper;
import org.opensearch.client.json.JsonpSerializable;
import org.opensearch.client.json.ObjectBuilderDeserializer;
import org.opensearch.client.json.ObjectDeserializer;
import org.opensearch.client.util.ApiTypeHelper;
import org.opensearch.client.util.MissingRequiredPropertyException;
import org.opensearch.client.util.ObjectBuilder;
import org.opensearch.client.util.ObjectBuilderBase;

// typedef: nodes.info.NodeInfoBootstrap

@JsonpDeserializable
public class NodeInfoBootstrap implements JsonpSerializable {
    private final String memoryLock;

    // ---------------------------------------------------------------------------------------------

    private NodeInfoBootstrap(Builder builder) {

        String memoryLock;
        try {
            memoryLock = ApiTypeHelper.requireNonNull(builder.memoryLock, this, "memoryLock");
        } catch (MissingRequiredPropertyException e) {
            memoryLock = "false";
        }
        this.memoryLock = memoryLock;

    }

    public static NodeInfoBootstrap of(Function<Builder, ObjectBuilder<NodeInfoBootstrap>> fn) {
        return fn.apply(new Builder()).build();
    }

    /**
     * Required - API name: {@code memory_lock}
     */
    public final String memoryLock() {
        return this.memoryLock;
    }

    /**
     * Serialize this object to JSON.
     */
    public void serialize(JsonGenerator generator, JsonpMapper mapper) {
        generator.writeStartObject();
        serializeInternal(generator, mapper);
        generator.writeEnd();
    }

    protected void serializeInternal(JsonGenerator generator, JsonpMapper mapper) {

        generator.writeKey("memory_lock");
        generator.write(this.memoryLock);

    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Builder for {@link NodeInfoBootstrap}.
     */

    public static class Builder extends ObjectBuilderBase implements ObjectBuilder<NodeInfoBootstrap> {
        private String memoryLock;

        /**
         * Required - API name: {@code memory_lock}
         */
        public final Builder memoryLock(String value) {
            this.memoryLock = value;
            return this;
        }

        /**
         * Builds a {@link NodeInfoBootstrap}.
         *
         * @throws NullPointerException if some of the required fields are null.
         */
        public NodeInfoBootstrap build() {
            _checkSingleUse();

            return new NodeInfoBootstrap(this);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Json deserializer for {@link NodeInfoBootstrap}
     */
    public static final JsonpDeserializer<NodeInfoBootstrap> _DESERIALIZER = ObjectBuilderDeserializer.lazy(
            Builder::new,
            NodeInfoBootstrap::setupNodeInfoBootstrapDeserializer
    );

    protected static void setupNodeInfoBootstrapDeserializer(ObjectDeserializer<NodeInfoBootstrap.Builder> op) {

        op.add(Builder::memoryLock, JsonpDeserializer.stringDeserializer(), "memory_lock");

    }

}
