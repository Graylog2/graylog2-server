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
package org.graylog.events.search;

import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.graylog.events.configuration.EventsConfiguration;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog2.cluster.ClusterConfigChangedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.graylog2.shared.security.RestPermissions.EVENT_DEFINITIONS_READ;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Guards the resolution contract of the factory: the enforcement setting may be held between calls, but the
 * subject's permissions must be re-evaluated on every call so grant changes take effect immediately.
 */
class EventDefinitionFilterFactoryTest {
    private static final ObjectId DEF_A = new ObjectId("000000000000000000000a01");
    private static final ObjectId DEF_B = new ObjectId("000000000000000000000b01");

    private final EventsConfigurationProvider configurationProvider = mock(EventsConfigurationProvider.class);
    private final DBEventDefinitionService eventDefinitionService = mock(DBEventDefinitionService.class);

    private EventDefinitionFilterFactory factory(boolean enforce) {
        enforcement(enforce);
        when(eventDefinitionService.findPermittedIds(any())).thenAnswer(invocation -> {
            final Predicate<String> idIsPermitted = invocation.getArgument(0);
            return Stream.of(DEF_A, DEF_B)
                    .filter(id -> idIsPermitted.test(id.toHexString()))
                    .toList();
        });
        return new EventDefinitionFilterFactory(configurationProvider, eventDefinitionService);
    }

    private void enforcement(boolean enforce) {
        when(configurationProvider.get())
                .thenReturn(EventsConfiguration.builder().enforceEventDefinitionPermissions(enforce).build());
    }

    private static Subject subjectPermittedFor(ObjectId... definitionIds) {
        final Subject subject = mock(Subject.class);
        when(subject.isPermitted(EVENT_DEFINITIONS_READ)).thenReturn(false);
        when(subject.isPermitted(EVENT_DEFINITIONS_READ + ":" + DEF_A.toHexString())).thenReturn(false);
        when(subject.isPermitted(EVENT_DEFINITIONS_READ + ":" + DEF_B.toHexString())).thenReturn(false);
        for (ObjectId id : definitionIds) {
            when(subject.isPermitted(EVENT_DEFINITIONS_READ + ":" + id.toHexString())).thenReturn(true);
        }
        return subject;
    }

    @Test
    void returnsAllAllowedWhenEnforcementIsDisabled() {
        final EventDefinitionFilter filter = factory(false).forSubject(subjectPermittedFor());

        assertThat(filter.isAllAllowed()).isTrue();
    }

    @Test
    void doesNotEnumerateDefinitionsWhenEnforcementIsDisabled() {
        factory(false).forSubject(subjectPermittedFor());

        verify(eventDefinitionService, never()).findPermittedIds(any());
    }

    @Test
    void returnsAllAllowedForASubjectWithBlanketReadPermission() {
        final Subject admin = mock(Subject.class);
        when(admin.isPermitted(EVENT_DEFINITIONS_READ)).thenReturn(true);

        final EventDefinitionFilter filter = factory(true).forSubject(admin);

        assertThat(filter.isAllAllowed()).isTrue();
        verify(eventDefinitionService, never()).findPermittedIds(any());
    }

    @Test
    void allowsOnlyTheDefinitionsTheSubjectMayRead() {
        final EventDefinitionFilter filter = factory(true).forSubject(subjectPermittedFor(DEF_A));

        assertThat(filter.isAllAllowed()).isFalse();
        assertThat(filter.eventDefinitionIds()).containsExactly(DEF_A.toHexString());
    }

    @Test
    void allowsNothingForASubjectWithoutAnyGrant() {
        final EventDefinitionFilter filter = factory(true).forSubject(subjectPermittedFor());

        assertThat(filter.isAllAllowed()).isFalse();
        assertThat(filter.eventDefinitionIds()).isEmpty();
    }

    /**
     * A grant added or revoked between two calls must be reflected in the very next filter.
     */
    @Test
    void reevaluatesSubjectPermissionsOnEveryCall() {
        final EventDefinitionFilterFactory factory = factory(true);

        assertThat(factory.forSubject(subjectPermittedFor(DEF_A)).eventDefinitionIds())
                .containsExactly(DEF_A.toHexString());
        assertThat(factory.forSubject(subjectPermittedFor(DEF_A, DEF_B)).eventDefinitionIds())
                .containsExactlyInAnyOrder(DEF_A.toHexString(), DEF_B.toHexString());
        assertThat(factory.forSubject(subjectPermittedFor()).eventDefinitionIds())
                .isEmpty();
    }

    /**
     * The enforcement setting is read once and then held, so the factory has to pick up a change from the
     * cluster config event rather than by re-reading on every search.
     */
    @Test
    void picksUpTheEnforcementSettingFromAClusterConfigChange() {
        final EventDefinitionFilterFactory factory = factory(false);
        assertThat(factory.forSubject(subjectPermittedFor(DEF_A)).isAllAllowed()).isTrue();

        enforcement(true);
        factory.handleUpdatedClusterConfig(clusterConfigChanged(EventsConfiguration.class.getCanonicalName()));

        assertThat(factory.forSubject(subjectPermittedFor(DEF_A)).eventDefinitionIds())
                .containsExactly(DEF_A.toHexString());
    }

    @Test
    void ignoresClusterConfigChangesForOtherConfigurationTypes() {
        final EventDefinitionFilterFactory factory = factory(false);

        enforcement(true);
        factory.handleUpdatedClusterConfig(clusterConfigChanged("org.graylog2.some.OtherConfiguration"));

        assertThat(factory.forSubject(subjectPermittedFor(DEF_A)).isAllAllowed()).isTrue();
    }

    private static ClusterConfigChangedEvent clusterConfigChanged(String type) {
        return ClusterConfigChangedEvent.create(DateTime.now(DateTimeZone.UTC), "node-id", type);
    }

    /**
     * The allow-list is handed to the storage adapters, so it must not be mutable in flight.
     */
    @Test
    void returnsAnImmutableAllowList() {
        final Set<String> ids = factory(true).forSubject(subjectPermittedFor(DEF_A)).eventDefinitionIds();

        assertThat(catchThrowable(() -> ids.add("something-else")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
