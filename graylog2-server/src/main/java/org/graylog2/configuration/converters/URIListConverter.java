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
        checkNotNull(value, "URI List must not be null.");

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
        checkNotNull(value, "URI List must not be null.");
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
