package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

public class ListOfURIsWithHostAndSchemeValidator implements Validator<List<URI>> {
    @Override
    public void validate(String name, List<URI> value) throws ValidationException {
        final List<URI> invalidUris = value.stream()
            .filter(uri -> uri.getHost() == null || uri.getScheme() == null)
            .collect(Collectors.toList());

        if (!invalidUris.isEmpty()) {
            throw new ValidationException("Parameter " + name + " must not contain URIs without host or scheme. (found " + invalidUris + ")");
        }
    }
}
