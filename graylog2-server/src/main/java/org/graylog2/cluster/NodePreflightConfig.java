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
package org.graylog2.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;
import java.util.List;

@AutoValue
@JsonDeserialize(builder = NodePreflightConfig.Builder.class)
@WithBeanGetter
public abstract class  NodePreflightConfig {
    public enum State {
        UNCONFIGURED, // first start of a DataNode
        CONFIGURED, // the DataNode has been configured by the Preflight UI
        CSR, // DataNode created the CSR
        SIGNED, // Graylog CA signed the CSR
        CONNECTED, // DataNode started with the certificate
        ERROR // sh*t happened
    }

    public static final String FIELD_ID = "id";
    public static final String FIELD_NODEID = "node_id";
    public static final String FIELD_ALTNAMES = "alt_names";
    public static final String FIELD_VALIDFOR = "valid_for";
    public static final String FIELD_STATE = "state";
    public static final String FIELD_ERRORMSG = "error_msg";
    public static final String FIELD_CSR = "csr";
    public static final String FIELD_CERTIFICATE = "certificate";

    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty
    @Nullable
    public abstract String nodeId();

    @JsonProperty
    @Nullable
    public abstract List<String> altNames();

    @JsonProperty
    @Nullable
    public abstract Integer validFor();

    @JsonProperty
    @Nullable
    public abstract State state();

    @JsonProperty
    @Nullable
    public abstract String errorMsg();

    @JsonProperty
    @Nullable
    public abstract String csr();

    @JsonProperty
    @Nullable
    public abstract String certificate();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @ObjectId
        @Id
        @JsonProperty(FIELD_ID)
        public abstract Builder id(String id);

        @JsonProperty(FIELD_NODEID)
        public abstract Builder nodeId(String nodeId);

        @JsonProperty(FIELD_ALTNAMES)
        public abstract Builder altNames(List<String> altNames);

        @JsonProperty(FIELD_VALIDFOR)
        public abstract Builder validFor(Integer validFor);

        @JsonProperty(FIELD_STATE)
        public abstract Builder state(State state);

        @JsonProperty(FIELD_ERRORMSG)
        public abstract Builder errorMsg(String errorMsg);

        @JsonProperty(FIELD_CSR)
        public abstract Builder csr(String csr);

        @JsonProperty(FIELD_CERTIFICATE)
        public abstract Builder certificate(String cert);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_NodePreflightConfig.Builder();
        }

        public abstract NodePreflightConfig build();
    }
}
