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
package org.graylog2.inputs.codecs;

import com.google.inject.Inject;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.codecs.Codec;

import java.util.Map;

public class CodecFactory {
    private Map<String, Codec.Factory<? extends Codec>> codecFactory;

    @Inject
    public CodecFactory(Map<String, Codec.Factory<? extends Codec>> codecFactory) {
        this.codecFactory = codecFactory;
    }

    public Map<String, Codec.Factory<? extends Codec>> getFactory() {
        return codecFactory;
    }

    public Codec create(String type, Configuration configuration) {
        final Codec.Factory<? extends Codec> factory = this.codecFactory.get(type);

        if (factory == null) {
            throw new IllegalArgumentException("Codec type " + type + " does not exist.");
        }

        return factory.create(configuration);
    }
}
