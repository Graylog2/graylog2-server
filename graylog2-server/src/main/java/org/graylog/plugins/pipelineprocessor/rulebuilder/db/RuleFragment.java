/*
 *  Copyright (C) 2020 Graylog, Inc.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the Server Side Public License, version 1,
 *  as published by MongoDB, Inc.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  Server Side Public License for more details.
 *
 *  You should have received a copy of the Server Side Public License
 *  along with this program. If not, see
 *  <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.pipelineprocessor.rulebuilder.db;

import com.google.auto.value.AutoValue;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;

import javax.annotation.Nullable;

@AutoValue
public abstract class RuleFragment {

    @Nullable
    public abstract String fragment();

    public abstract FunctionDescriptor descriptor();

    public static RuleFragment create(String fragment, FunctionDescriptor descriptor) {
        return builder()
                .fragment(fragment)
                .descriptor(descriptor)
                .build();
    }


    public static Builder builder() {
        return new AutoValue_RuleFragment.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder descriptor(FunctionDescriptor descriptor);

        public abstract Builder fragment(String fragment);

        public abstract RuleFragment build();
    }
}
