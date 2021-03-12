package org.graylog2.configuration.converters;

import com.github.joschi.jadconfig.Converter;
import com.github.joschi.jadconfig.ParameterException;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class PathSetConverter implements Converter<Set<Path>> {
    protected static final String SEPARATOR = ",";

    @Override
    public Set<Path> convertFrom(String value) {
        if (value == null) {
            throw new ParameterException("Path list must not be null.");
        }

        return Arrays.stream(value.split(SEPARATOR))
                     .map(StringUtils::trimToNull)
                     .filter(Objects::nonNull)
                     .map(Paths::get)
                     .collect(Collectors.toSet());
    }

    @Override
    public String convertTo(Set<Path> value) {
        if (value == null) {
            throw new ParameterException("String list of Paths must not be null.");
        }

        return value.stream().map(Path::toString).collect(Collectors.joining(","));
    }
}
