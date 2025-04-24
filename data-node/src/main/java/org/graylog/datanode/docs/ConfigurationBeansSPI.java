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
package org.graylog.datanode.docs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class ConfigurationBeansSPI {
    public static List<Object> loadConfigurationBeans() {
        final ServiceLoader<DocumentedBeansService> configurationBeansLoader = ServiceLoader.load(DocumentedBeansService.class);
        final Iterator<DocumentedBeansService> iterator = configurationBeansLoader.iterator();
        List<Object> configurationBeans = new ArrayList<>();
        while (iterator.hasNext()) {
            final DocumentedBeansService service = iterator.next();
            configurationBeans.addAll(service.getDocumentedConfigurationBeans());
        }
        return configurationBeans;
    }
}
