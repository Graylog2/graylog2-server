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
package org.graylog2.bindings.providers;

import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.SimpleObjectWrapper;
import freemarker.template.Version;

import jakarta.inject.Provider;
import jakarta.inject.Singleton;

/**
 * Provide a freemarker configuration with sane security defaults.
 * For Reference see <a href="https://freemarker.apache.org/docs/app_faq.html#faq_template_uploading_security">
 * Freemarker Uploading Security Guide
 * </a>
 */
@Singleton
public class SecureFreemarkerConfigProvider implements Provider<Configuration> {
    public static final Version VERSION = Configuration.VERSION_2_3_31;

    @Override
    public Configuration get() {
        final Configuration configuration = new Configuration(VERSION);

        configuration.setNewBuiltinClassResolver(TemplateClassResolver.ALLOWS_NOTHING_RESOLVER);
        final SimpleObjectWrapper simpleObjectWrapper = new SimpleObjectWrapper(VERSION);
        simpleObjectWrapper.setDOMNodeSupport(false);
        configuration.setObjectWrapper(simpleObjectWrapper);

        return configuration;
    }
}
