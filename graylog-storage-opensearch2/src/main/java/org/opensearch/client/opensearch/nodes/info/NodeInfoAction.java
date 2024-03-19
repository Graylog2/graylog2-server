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
package org.opensearch.client.opensearch.nodes.info;

import jakarta.json.stream.JsonGenerator;
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

import java.util.function.Function;

// typedef: nodes.info.NodeInfoAction

@JsonpDeserializable
public class NodeInfoAction implements JsonpSerializable {
    private final String destructiveRequiresName;

    // ---------------------------------------------------------------------------------------------

    private NodeInfoAction(Builder builder) {

        String destructiveRequiresNameSetting;
        try {
            destructiveRequiresNameSetting = ApiTypeHelper.requireNonNull(builder.destructiveRequiresName, this, "destructiveRequiresName");
        } catch (MissingRequiredPropertyException e) {
            destructiveRequiresNameSetting = "true";
        }
        this.destructiveRequiresName = destructiveRequiresNameSetting;

    }

    public static NodeInfoAction of(Function<Builder, ObjectBuilder<NodeInfoAction>> fn) {
        return fn.apply(new Builder()).build();
    }

    /**
     * Required - API name: {@code destructive_requires_name}
     */
    public final String destructiveRequiresName() {
        return this.destructiveRequiresName;
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

        generator.writeKey("destructive_requires_name");
        generator.write(this.destructiveRequiresName);

    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Builder for {@link NodeInfoAction}.
     */

    public static class Builder extends ObjectBuilderBase implements ObjectBuilder<NodeInfoAction> {
        private String destructiveRequiresName;

        /**
         * Required - API name: {@code destructive_requires_name}
         */
        public final Builder destructiveRequiresName(String value) {
            this.destructiveRequiresName = value;
            return this;
        }

        /**
         * Builds a {@link NodeInfoAction}.
         *
         * @throws NullPointerException if some of the required fields are null.
         */
        public NodeInfoAction build() {
            _checkSingleUse();

            return new NodeInfoAction(this);
        }
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Json deserializer for {@link NodeInfoAction}
     */
    public static final JsonpDeserializer<NodeInfoAction> _DESERIALIZER = ObjectBuilderDeserializer.lazy(
            Builder::new,
            NodeInfoAction::setupNodeInfoActionDeserializer
    );

    protected static void setupNodeInfoActionDeserializer(ObjectDeserializer<NodeInfoAction.Builder> op) {

        op.add(Builder::destructiveRequiresName, JsonpDeserializer.stringDeserializer(), "destructive_requires_name");

    }

}
