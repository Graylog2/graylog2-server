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
package org.graylog.datanode;

import com.github.joschi.jadconfig.Parameter;
import org.bson.assertions.Assertions;
import org.graylog.datanode.commands.Server;
import org.graylog2.configuration.Documentation;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

class ConfigurationDocumentationTest {

    @Test
    void testAllFieldsAreDocumented() {
        final List<Object> datanodeConfiguration = new Server().getCommandConfigurationBeans();
        final List<Field> undocumentedFields = datanodeConfiguration.stream().flatMap(configurationBean -> {
            return Arrays.stream(configurationBean.getClass().getDeclaredFields())
                    .filter(f -> f.isAnnotationPresent(Parameter.class))
                    .filter(f -> !f.isAnnotationPresent(Documentation.class));
        }).toList();


        if (!undocumentedFields.isEmpty()) {
            final String fields = undocumentedFields.stream()
                    .map(Field::toString)
                    .collect(Collectors.joining("\n"));
            Assertions.fail("Following datanode configuration fields require @Documentation annotation: \n" + fields);
        }
    }
}
