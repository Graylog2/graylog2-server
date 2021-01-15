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
package org.graylog2.grok;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.SuppressForbidden;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;

public class InMemoryGrokPatternServiceTest {

    private InMemoryGrokPatternService service;

    @Before
    @SuppressForbidden("Using Executors.newSingleThreadExecutor() is okay in tests")
    public void setup() {
        final ClusterEventBus clusterEventBus = new ClusterEventBus("cluster-event-bus", Executors.newSingleThreadExecutor());
        service = new InMemoryGrokPatternService(clusterEventBus);
    }

    @Test
    public void load() throws Exception {
        final GrokPattern pattern = service.save(GrokPattern.create("NAME", ".*"));

        final GrokPattern loaded = service.load(pattern.id());

        assertThat(loaded)
                .isNotNull()
                .isEqualTo(pattern);

        assertThatExceptionOfType(NotFoundException.class)
                .isThrownBy(() -> service.load("whatever"))
                .withMessage("Couldn't find Grok pattern with ID whatever");
    }

    @Test
    public void loadAll() throws Exception {
        GrokPattern pattern1 = service.save(GrokPattern.create("NAME1", ".*"));
        GrokPattern pattern2 = service.save(GrokPattern.create("NAME2", ".*"));
        GrokPattern pattern3 = service.save(GrokPattern.create("NAME3", ".*"));

        assertThat(service.loadAll()).containsExactlyInAnyOrder(pattern1, pattern2, pattern3);
    }

    @Test
    public void bulkLoad() throws Exception {
        GrokPattern pattern1 = service.save(GrokPattern.create("NAME1", ".*"));
        GrokPattern pattern2 = service.save(GrokPattern.create("NAME2", ".*"));
        GrokPattern pattern3 = service.save(GrokPattern.create("NAME3", ".*"));

        assertThat(service.bulkLoad(ImmutableSet.of(pattern1.id(), pattern3.id()))).containsExactlyInAnyOrder(pattern1, pattern3);
    }

    @Test
    public void save() throws Exception {
        // new pattern
        final GrokPattern pattern = service.save(GrokPattern.create("NEW", ".*"));

        assertThat(pattern).isNotNull();
        assertThat(pattern.id()).isNotEmpty();

        // check that updating works
        final GrokPattern updated = service.save(pattern.toBuilder().name("OTHERNAME").build());

        final GrokPattern loaded = service.load(pattern.id());

        assertThat(loaded).isEqualTo(updated);

        //check that using stored patterns works
        final GrokPattern newPattern = service.save(GrokPattern.create("NEWONE", "%{OTHERNAME}"));

        final GrokPattern newLoaded = service.load(newPattern.id());

        assertThat(newLoaded).isEqualTo(newPattern);


        // save should validate
        try {
            service.save(GrokPattern.create("INVALID", "*"));
            fail("Should throw ValidationException");
        } catch (ValidationException ignored) {
        }
    }

    @Test
    public void saveAll() throws Exception {
        Collection<GrokPattern> patterns = ImmutableList.of(GrokPattern.create("1", ".*"),
                                                            GrokPattern.create("2", ".+"));
        final List<GrokPattern> saved = service.saveAll(patterns, false);
        assertThat(saved).hasSize(2);

        service.saveAll(patterns, false);
        // should have added the patterns again
        assertThat(service.loadAll()).hasSize(4);

        // replaced all patterns
        service.saveAll(patterns, true);
        assertThat(service.loadAll()).hasSize(2);
    }

    @Test
    public void validateAll() throws Exception {
        Collection<GrokPattern> patternsRight = ImmutableList.of(GrokPattern.create("1", ".*"),
                GrokPattern.create("2", ".+"));
        final boolean result = service.validateAll(patternsRight);
        assertThat(result).isTrue();

        Collection<GrokPattern> patternsWrong = ImmutableList.of(GrokPattern.create("1", "***"),
                GrokPattern.create("2", ".+"));
        try {
            service.validateAll(patternsWrong);
            fail("Should throw ValidationException");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void delete() throws Exception {
        final GrokPattern saved = service.save(GrokPattern.create("NAME", "name"));
        try {
            service.load(saved.id());
        } catch (Exception e) {
            fail("Should not throw");
        }
        assertThat(service.delete(saved.id())).isEqualTo(1);

        try {
            service.load(saved.id());
            fail("Should throw");
        } catch (NotFoundException ignored) {
        } catch (Exception e) {
            fail("Should not throw any other exceptions");
        }
    }

    @Test
    public void deleteAll() throws Exception {
        Collection<GrokPattern> patterns = ImmutableList.of(GrokPattern.create("1", ".*"),
                                                            GrokPattern.create("2", ".+"));
        final List<GrokPattern> saved = service.saveAll(patterns, false);
        assertThat(service.deleteAll()).isEqualTo(2);
        assertThat(service.loadAll()).isEmpty();
    }

    @Test
    public void match() throws Exception {
        final String name = "IP";
        final String sampleData = "1.2.3.4";
        final Map<String, Object> expectedResult = Collections.singletonMap("IP", "1.2.3.4");
        GrokPattern grokPattern = GrokPattern.create(name, "\\d.\\d.\\d.\\d");
        final Map<String, Object> result = service.match(grokPattern, sampleData);
        assertThat(result).isEqualTo(expectedResult);
    }
}