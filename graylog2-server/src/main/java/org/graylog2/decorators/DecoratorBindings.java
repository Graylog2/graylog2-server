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
package org.graylog2.decorators;

import com.google.inject.multibindings.MapBinder;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.plugin.inject.Graylog2Module;

public class DecoratorBindings extends Graylog2Module {
    @Override
    protected void configure() {
        final MapBinder<String, SearchResponseDecorator.Factory> searchResponseDecoratorBinder = searchResponseDecoratorBinder();
        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       SyslogSeverityMapperDecorator.class,
                                       SyslogSeverityMapperDecorator.Factory.class);

        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       FormatStringDecorator.class,
                                       FormatStringDecorator.Factory.class);

        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       LookupTableDecorator.class,
                                       LookupTableDecorator.Factory.class);

        installSearchResponseDecorator(searchResponseDecoratorBinder,
                                       LinkFieldDecorator.class,
                                       LinkFieldDecorator.Factory.class);
    }
}
