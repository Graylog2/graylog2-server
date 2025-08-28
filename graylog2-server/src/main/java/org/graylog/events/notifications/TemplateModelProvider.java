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
package org.graylog.events.notifications;

import com.google.common.collect.ImmutableMap;
import jakarta.inject.Inject;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.jackson.TypeReferences;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.web.customization.CustomizationConfig;
import org.joda.time.DateTimeZone;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class TemplateModelProvider {
    private final CustomizationConfig customizationConfig;
    private final ObjectMapperProvider objectMapperProvider;
    private final URI httpExternalUri;

    @Inject
    public TemplateModelProvider(CustomizationConfig customizationConfig, ObjectMapperProvider objectMapperProvider, HttpConfiguration httpConfiguration) {
        this.customizationConfig = customizationConfig;
        this.objectMapperProvider = objectMapperProvider;
        this.httpExternalUri = httpConfiguration.getHttpExternalUri();

    }

    public Map<String, Object> of(EventNotificationContext ctx, List<MessageSummary> backlog, DateTimeZone timeZone) {
        return of(ctx, backlog, timeZone, Map.of());
    }

    public Map<String, Object> of(EventNotificationContext ctx, List<MessageSummary> backlog, DateTimeZone timeZone, Map<String, Object> customFields) {
        EventNotificationModelData modelData = EventNotificationModelData.of(ctx, backlog);

        return ImmutableMap.<String, Object>builder()
                .putAll(customFields)
                .putAll(objectMapperProvider.getForTimeZone(timeZone).convertValue(modelData, TypeReferences.MAP_STRING_OBJECT))
                .put("http_external_uri", this.httpExternalUri)
                .put("product_name", customizationConfig.productName())
                .build();
    }
}
