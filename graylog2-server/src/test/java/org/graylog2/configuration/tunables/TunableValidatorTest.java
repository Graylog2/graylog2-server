/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.configuration.tunables;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.plugin.validate.ClusterConfigValidatorService;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TunableValidatorTest {
    public record RecordTunable(@JsonProperty("min_length") @Min(6) int minLength) {}

    public record Conditional(@JsonProperty("enabled") boolean enabled,
                              @JsonProperty("path") @TunableField(label = "Path", enabledWhen = @When(property = "enabled", in = "true")) @NotBlank String path) {}

    private final ObjectMapper objectMapper = new ObjectMapperProvider().get();
    private final RuntimeTunableSchemaGenerator schemas = new RuntimeTunableSchemaGenerator(objectMapper);
    private final TunableValidator validator = new TunableValidator(objectMapper, new ClusterConfigValidatorService(Map.of()));

    private ValidationResult validate(Class<?> type, Object config) throws Exception {
        return validator.validate(type, schemas.generate(type), config);
    }

    @Test
    void reportsRecordViolationOnceUnderJsonName() throws Exception {
        final var result = validate(RecordTunable.class, new RecordTunable(3));

        assertThat(result.getErrors()).containsOnlyKeys("min_length");
        assertThat(result.getErrors().get("min_length")).hasSize(1);
    }

    @Test
    void skipsConstraintsOfInactiveFields() throws Exception {
        assertThat(validate(Conditional.class, new Conditional(false, "")).failed()).isFalse();
        assertThat(validate(Conditional.class, new Conditional(true, "")).getErrors()).containsOnlyKeys("path");
    }

    @Test
    void reportsClusterConfigValidatorFailureAsGlobalError() throws Exception {
        final var withValidator = new TunableValidator(objectMapper,
                new ClusterConfigValidatorService(Map.of(RecordTunable.class, config -> {
                    throw new ConfigValidationException("nope");
                })));

        final var result = withValidator.validate(RecordTunable.class, schemas.generate(RecordTunable.class), new RecordTunable(8));

        assertThat(result.getErrors().get(TunableValidator.GLOBAL_ERROR_KEY)).containsExactly("nope");
    }

    public record Unannotated(boolean someFlag,
                              @TunableField(label = "Path", enabledWhen = @When(property = "some_flag", in = "true")) @NotBlank String filePath) {}

    public static class GetterBean {
        private final int count;

        @JsonCreator
        public GetterBean(@JsonProperty("count") int count) {
            this.count = count;
        }

        @JsonProperty("count")
        @Min(1)
        public int getCount() {
            return count;
        }
    }

    @Test
    void skipsInactiveFieldsWithoutExplicitJsonNames() throws Exception {
        assertThat(validate(Unannotated.class, new Unannotated(false, "")).failed()).isFalse();
        assertThat(validate(Unannotated.class, new Unannotated(true, "")).getErrors()).containsOnlyKeys("file_path");
    }

    @Test
    void usesBeanPropertyNamesForGetters() throws Exception {
        assertThat(validate(GetterBean.class, new GetterBean(0)).getErrors()).containsOnlyKeys("count");
    }
}
