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
