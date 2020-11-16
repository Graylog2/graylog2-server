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
package org.graylog2.plugin.decorators;

import org.graylog2.decorators.Decorator;
import org.graylog2.plugin.DescriptorWithHumanName;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import java.util.function.Function;

@FunctionalInterface
public interface SearchResponseDecorator extends Function<SearchResponse, SearchResponse> {
    interface Factory {
        SearchResponseDecorator create(Decorator decorator);
        Config getConfig();
        Descriptor getDescriptor();
    }

    interface Config {
        ConfigurationRequest getRequestedConfiguration();
    }

    abstract class Descriptor extends DescriptorWithHumanName {
        public Descriptor(String name, String linkToDocs, String humanName) {
            super(name, false, linkToDocs, humanName);
        }
    }
}
