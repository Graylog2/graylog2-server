/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.inputs;

import org.graylog2.plugin.inputs.MessageInput;

import java.util.Map;

public class InputDescription {
    private final MessageInput.Descriptor descriptor;
    private final MessageInput.Config config;

    public InputDescription(MessageInput.Descriptor descriptor, MessageInput.Config config) {
        this.descriptor = descriptor;
        this.config = config;
    }

    public String getName() {
        return descriptor.getName();
    }

    public boolean isExclusive() {
        return descriptor.isExclusive();
    }

    public String getLinkToDocs() {
        return descriptor.getLinkToDocs();
    }

    public Map<String, Map<String, Object>> getRequestedConfiguration() {
        return config.getRequestedConfiguration().asList();
    }
}
