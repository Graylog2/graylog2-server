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

@AutoValue
public abstract class OptionsTemplateRecord {

    public abstract int templateId();

    public abstract ImmutableList<InformationElement> scopeFields();

    public abstract ImmutableList<InformationElement> optionFields();

    public static Builder builder() {
        return new AutoValue_OptionsTemplateRecord.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder templateId(int templateId);

        public abstract Builder scopeFields(ImmutableList<InformationElement> scopeFields);

        public abstract ImmutableList.Builder<InformationElement> scopeFieldsBuilder();

        public abstract Builder optionFields(ImmutableList<InformationElement> optionFields);

        public abstract ImmutableList.Builder<InformationElement> optionFieldsBuilder();

        public abstract OptionsTemplateRecord build();
    }
}
