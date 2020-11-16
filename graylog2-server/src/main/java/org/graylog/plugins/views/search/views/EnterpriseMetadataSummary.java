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
package org.graylog.plugins.views.search.views;

import com.github.zafarkhaja.semver.Version;

import javax.inject.Singleton;
import java.net.URI;

@Singleton
public class EnterpriseMetadataSummary extends PluginMetadataSummary {
    @Override
    public String uniqueId() {
        return "org.graylog.plugins.enterprise.EnterprisePlugin";
    }

    @Override
    public String name() {
        return "Graylog Enterprise";
    }

    @Override
    public String author() {
        return "Graylog, Inc.";
    }

    @Override
    public URI url() {
        return URI.create("https://www.graylog.org/enterprise");
    }

    @Override
    public Version version() {
        return Version.valueOf("3.1.0");
    }

    @Override
    public String description() {
        return "Graylog Enterprise";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EnterpriseMetadataSummary;
    }
}
