/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
