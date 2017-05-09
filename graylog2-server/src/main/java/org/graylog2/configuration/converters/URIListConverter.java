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
package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;

public class URIListConverter implements Converter<List<URI>> {
    private static final String SEPARATOR = ",";

    @Override
    public List<URI> convertFrom(String value) {
        if (value == null) {
            throw new ParameterException("URI List must not be null.");
        }

        final Iterable<String> splittedUris = Splitter.on(SEPARATOR)
            .omitEmptyStrings()
            .trimResults()
            .split(value);

        return StreamSupport.stream(splittedUris.spliterator(), false)
            .map(this::constructURIFromString)
            .collect(Collectors.toList());
    }

    @Override
    public String convertTo(List<URI> value) {
        if (value == null) {
            throw new ParameterException("URI List must not be null.");
        }

        return Joiner.on(SEPARATOR)
            .skipNulls()
            .join(value);
    }

    private URI constructURIFromString(String value) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new ParameterException(e.getMessage(), e);
        }
    }
}
