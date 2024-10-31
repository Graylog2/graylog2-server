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
package org.graylog.integrations.ipfix;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.Set;

@AutoValue
public abstract class IpfixMessage {

    public static Builder builder() {
        return new AutoValue_IpfixMessage.Builder();
    }

    public abstract ImmutableList<TemplateRecord> templateRecords();

    public abstract ImmutableList<OptionsTemplateRecord> optionsTemplateRecords();

    public abstract ImmutableList<Flow> flows();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder templateRecords(ImmutableList<TemplateRecord> templateRecords);

        public abstract ImmutableList.Builder<TemplateRecord> templateRecordsBuilder();

        public abstract Builder optionsTemplateRecords(ImmutableList<OptionsTemplateRecord> optionsTemplateRecords);

        public abstract ImmutableList.Builder<OptionsTemplateRecord> optionsTemplateRecordsBuilder();

        public abstract Builder flows(ImmutableList<Flow> flows);

        public abstract ImmutableList.Builder<Flow> flowsBuilder();

        public abstract IpfixMessage build();

        public Builder addAllTemplates(Set<TemplateRecord> templateRecords) {
            templateRecordsBuilder().addAll(templateRecords);
            return this;
        }

        public Builder addAllOptionsTemplateSet(Set<OptionsTemplateRecord> optionsTemplateRecords) {
            optionsTemplateRecordsBuilder().addAll(optionsTemplateRecords);
            return this;
        }

        public Builder addAllFlows(Set<Flow> flows) {
            flowsBuilder().addAll(flows);
            return this;
        }
    }
}
