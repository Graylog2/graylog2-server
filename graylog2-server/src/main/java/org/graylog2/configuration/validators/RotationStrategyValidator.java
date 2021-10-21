package org.graylog2.configuration.validators;

import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.google.common.collect.Sets;
import org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy;
import org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy;
import org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy;

import java.util.Set;
import java.util.stream.Collectors;

public class RotationStrategyValidator implements Validator<Set<String>> {
    Set<String> VALID_STRATEGIES = Sets.newHashSet(
            MessageCountRotationStrategy.strategyName, SizeBasedRotationStrategy.strategyName, TimeBasedRotationStrategy.strategyName);

    @Override
    // The set of valid rotation strategies must
    // - contain only names of supported strategies
    // - not be empty
    public void validate(String parameter, Set<String> value) throws ValidationException {
        if (value == null || value.isEmpty()) {
            throw new ValidationException("Parameter " + parameter + " should be non-empty list");
        }

        if (!value.stream()
                .filter(s -> !VALID_STRATEGIES.contains(s))
                .collect(Collectors.toSet()).isEmpty()) {
            throw new ValidationException("Parameter " + parameter + " contains invalid values: " + value);
        }
    }
}
