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
package org.graylog2.configuration.managed;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.Repository;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.info.ParameterInfoCollector;
import org.graylog2.configuration.ConfigurationInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ConfigurationReprocessorTest {
    private static final String FROM_FILE = "from_file";
    private static final String NULL_DEFAULT = "null_default";
    private static final String RENAMED = "renamed";
    private static final String OLD_NAME = "old_name";
    private static final String CHANGED_BY_CODE = "changed_by_code";
    private static final Set<String> RESETTABLE = Set.of(FROM_FILE, NULL_DEFAULT, RENAMED);

    public static class TestConfiguration {
        @Documentation("A parameter which the file configures")
        @Parameter(FROM_FILE)
        private String fromFile = "the default";

        @Documentation("A parameter without a default")
        @Parameter(NULL_DEFAULT)
        private String nullDefault;

        @Documentation("A parameter which can still be given by its old name")
        @Parameter(value = RENAMED, fallbackPropertyName = OLD_NAME)
        private int renamed = 1;

        @Documentation("A parameter which the code changes at runtime, like is_leader")
        @Parameter(CHANGED_BY_CODE)
        private boolean changedByCode = true;
    }

    private final TestConfiguration configuration = new TestConfiguration();
    private final Map<String, String> fileValues = new HashMap<>(Map.of(FROM_FILE, "from the file"));
    private final Map<String, String> storedValues = new HashMap<>();
    private ConfigurationReprocessor reprocessor;

    /**
     * Reads the given map each time it is opened, the way the repository of stored values does.
     */
    private static Repository reloading(Map<String, String> values) {
        return new Repository() {
            private Map<String, String> loaded = Map.of();

            @Override
            public void open() {
                loaded = Map.copyOf(values);
            }

            @Override
            public String read(String name) {
                return loaded.get(name);
            }

            @Override
            public void close() {
            }
        };
    }

    @BeforeEach
    void setUp() throws Exception {
        final var collector = new ParameterInfoCollector();
        final var jadConfig = new JadConfig(List.of(reloading(storedValues), reloading(fileValues)), configuration);
        jadConfig.addParameterListener(collector);
        jadConfig.process();
        reprocessor = new ConfigurationReprocessor(jadConfig, collector,
                new ConfigurationInfoService(collector.getParameterInfos()));
    }

    @Test
    void leavesParametersWithAValueToProcessing() throws Exception {
        storedValues.put(FROM_FILE, "stored");
        reprocessor.reprocess(RESETTABLE);
        assertThat(configuration.fromFile).isEqualTo("stored");

        storedValues.clear();
        reprocessor.reprocess(RESETTABLE);

        // The file still has a value, so the stored one is replaced by it and never by the default
        assertThat(configuration.fromFile).isEqualTo("from the file");
    }

    @Test
    void resetsAParameterWithoutADefaultToNull() throws Exception {
        storedValues.put(NULL_DEFAULT, "stored");
        reprocessor.reprocess(RESETTABLE);
        assertThat(configuration.nullDefault).isEqualTo("stored");

        storedValues.clear();
        reprocessor.reprocess(RESETTABLE);

        assertThat(configuration.nullDefault).isNull();
    }

    @Test
    void countsAValueGivenByTheFallbackName() throws Exception {
        fileValues.put(OLD_NAME, "5");
        reprocessor.reprocess(RESETTABLE);
        assertThat(configuration.renamed).isEqualTo(5);

        reprocessor.reprocess(RESETTABLE);
        assertThat(configuration.renamed).isEqualTo(5);

        fileValues.remove(OLD_NAME);
        reprocessor.reprocess(RESETTABLE);
        assertThat(configuration.renamed).isEqualTo(1);
    }

    @Test
    void leavesParametersAloneWhichCannotLoseTheirValueAtRuntime() throws Exception {
        configuration.changedByCode = false;

        reprocessor.reprocess(RESETTABLE);

        assertThat(configuration.changedByCode).isFalse();
    }
}
