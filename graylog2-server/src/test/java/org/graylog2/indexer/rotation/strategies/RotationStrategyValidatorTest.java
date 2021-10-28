package org.graylog2.indexer.rotation.strategies;

import com.github.joschi.jadconfig.ValidationException;
import org.graylog2.configuration.validators.RotationStrategyValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;

public class RotationStrategyValidatorTest {
    private final RotationStrategyValidator validator = new RotationStrategyValidator();
    private final String PARAM = "parameter-name";

    @Test
    void nullSet() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, null);
        });
    }

    @Test
    void emptySet() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, new HashSet<>());
        });
    }

    @Test
    void invalidStrategy() {
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, new HashSet<>(Arrays.asList("invalid-strategy")));
        });
        Assertions.assertThrows(ValidationException.class, () -> {
            validator.validate(PARAM, new HashSet<>(Arrays.asList(
                    TimeBasedRotationStrategy.strategyName,
                    "invalid-strategy")));
        });
    }

    @Test
    void validStrategy() throws ValidationException {
        validator.validate(PARAM, new HashSet<>(Arrays.asList(
                TimeBasedRotationStrategy.strategyName
        )));
        validator.validate(PARAM, new HashSet<>(Arrays.asList(
                TimeBasedRotationStrategy.strategyName,
                SizeBasedRotationStrategy.strategyName,
                MessageCountRotationStrategy.strategyName
        )));
    }
}
