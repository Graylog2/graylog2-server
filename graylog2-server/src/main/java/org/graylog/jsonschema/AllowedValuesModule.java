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
package org.graylog.jsonschema;

import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;

import java.util.Collection;

/** Adds the values of {@link AllowedValues} to the schema of the annotated property as its {@code enum}. */
public class AllowedValuesModule implements Module {
    @Override
    public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
        builder.forFields().withEnumResolver(AllowedValuesModule::allowedValues);
        builder.forMethods().withEnumResolver(AllowedValuesModule::allowedValues);
    }

    private static Collection<?> allowedValues(MemberScope<?, ?> member) {
        final AllowedValues allowedValues = member.getAnnotationConsideringFieldAndGetter(AllowedValues.class);
        return allowedValues == null ? null : AllowedValues.Values.of(allowedValues);
    }
}
