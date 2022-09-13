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
package org.graylog.plugins.views.search.rest;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.db.SearchDbService;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog2.indexer.fieldtypes.DiscoveredFieldTypeService;
import org.graylog2.indexer.fieldtypes.FieldTypes;
import org.graylog2.indexer.fieldtypes.MappedFieldTypesService;
import org.graylog2.plugin.indexer.searches.timeranges.RelativeRange;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@ExtendWith(MockitoExtension.class)
public class FieldTypesResourceTest {

    @Mock
    private DiscoveredFieldTypeService discoveredFieldTypeService;
    @Mock
    private SearchDbService searchDbService;

    @Test
    public void allFieldTypesChecksPermissionsForStream() {

        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream("4242")
                .denyStream("2323")
                .build();

        final MappedFieldTypesService fieldTypesService = (streamIds, timeRange) -> {
            // for each streamID return a field that's called exactly like the streamID
            return streamIds.stream().map(streamID -> MappedFieldTypeDTO.create(streamID, FieldTypes.Type.builder().type("text").build())).collect(Collectors.toSet());
        };

        final FieldTypesResource resource = new FieldTypesResource(fieldTypesService, discoveredFieldTypeService, searchDbService);
        final Set<MappedFieldTypeDTO> fields = resource.allFieldTypes(searchUser);

        // field for allowed stream has to be present
        assertThat(fields.stream().anyMatch(f -> f.name().equals("4242"))).isTrue();

        // field for denied stream must not be present
        assertThat(fields.stream().anyMatch(f -> f.name().equals("2323"))).isFalse();
    }

    @Test
    public void allFieldTypesReturnsResultFromMappedFieldTypesService() {
        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream("2323")
                .allowStream("4242")
                .build();

        final MappedFieldTypesService fieldTypesService = (streamIds, timeRange) -> {
            if (ImmutableSet.of("2323", "4242").equals(streamIds) && timeRange.equals(RelativeRange.allTime())) {
                return Collections.singleton(MappedFieldTypeDTO.create("foobar",
                        FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"))));
            } else {
                return Collections.emptySet();
            }
        };

        final FieldTypesResource resource = new FieldTypesResource(fieldTypesService, discoveredFieldTypeService, searchDbService);
        final Set<MappedFieldTypeDTO> result = resource.allFieldTypes(searchUser);

        assertThat(result)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(type -> {
                    assertThat(type.name()).isEqualTo("foobar");
                    assertThat(type.type().type()).isEqualTo("long");
                });
    }

    @Test
    public void passesRequestedTimeRangeToMappedFieldTypesService() throws Exception {
        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream("2323")
                .allowStream("4242")
                .build();

        final FieldTypesForStreamsRequest request = FieldTypesForStreamsRequest.Builder.builder()
                .streams(ImmutableSet.of("2323", "4242"))
                .timerange(RelativeRange.create(250))
                .build();

        final MappedFieldTypesService mappedFieldTypesService = (streamIds, timeRange) -> {
            if (timeRange.equals(RelativeRange.create(250))) {
                final FieldTypes.Type fieldType = FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"));
                final MappedFieldTypeDTO field = MappedFieldTypeDTO.create("foobar", fieldType);
                return Collections.singleton(field);
            } else {
                throw new AssertionError("Expected relative range of 250");
            }
        };

        final FieldTypesResource resource = new FieldTypesResource(mappedFieldTypesService, discoveredFieldTypeService, searchDbService);
        final Set<MappedFieldTypeDTO> result = resource.byStreams(request, searchUser);

        assertThat(result)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(type -> assertThat(type.name()).isEqualTo("foobar"));
    }

    @Test
    public void byStreamChecksPermissionsForStream() {
        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream("2323")
                .allowStream("4242")
                .build();

        final FieldTypesForStreamsRequest req = FieldTypesForStreamsRequest.Builder.builder()
                .streams(ImmutableSet.of("2323", "4242"))
                .build();

        final MappedFieldTypesService fieldTypesService = (streamIds, timeRange) -> {
            // for each streamID return a field that's called exactly like the streamID
            return streamIds.stream()
                    .map(streamID -> MappedFieldTypeDTO.create(streamID, FieldTypes.Type.builder().type("text").build()))
                    .collect(Collectors.toSet());
        };

        final FieldTypesResource resource = new FieldTypesResource(fieldTypesService, discoveredFieldTypeService, searchDbService);
        final Set<MappedFieldTypeDTO> fields = resource.byStreams(req, searchUser);

        assertThat(fields)
                .hasSize(2)
                .extracting(MappedFieldTypeDTO::name)
                .containsOnly("2323", "4242");
    }

    @Test
    public void byStreamReturnsTypesFromMappedFieldTypesService() {

        final SearchUser searchUser = TestSearchUser.builder()
                .allowStream("2323")
                .allowStream("4242")
                .build();

        final MappedFieldTypesService fieldTypesService = (streamIds, timeRange) -> {
            if (ImmutableSet.of("2323", "4242").equals(streamIds) && timeRange.equals(RelativeRange.allTime())) {
                final FieldTypes.Type fieldType = FieldTypes.Type.createType("long", ImmutableSet.of("numeric", "enumerable"));
                final MappedFieldTypeDTO field = MappedFieldTypeDTO.create("foobar", fieldType);
                return Collections.singleton(field);
            } else {
                throw new AssertionError("Expected allTime range and 2323, 4242 stream IDs");
            }
        };

        final FieldTypesForStreamsRequest request = FieldTypesForStreamsRequest.Builder.builder()
                .streams(ImmutableSet.of("2323", "4242"))
                .build();

        final Set<MappedFieldTypeDTO> result = new FieldTypesResource(fieldTypesService, discoveredFieldTypeService, searchDbService).byStreams(
                request,
                searchUser
        );

        assertThat(result)
                .hasSize(1)
                .extracting(MappedFieldTypeDTO::name)
                .containsOnly("foobar");
    }

    @Test
    public void shouldNotAllowAccessWithoutPermission() {
        final SearchUser searchUser = TestSearchUser
                .builder()
                .denyStream("2323")
                .allowStream("4242")
                .build();

        final FieldTypesForStreamsRequest req = FieldTypesForStreamsRequest.Builder.builder()
                .streams(ImmutableSet.of("2323", "4242"))
                .build();

        final FieldTypesResource resource = new FieldTypesResource((streamIds, timeRange) -> Collections.emptySet(), discoveredFieldTypeService, searchDbService);
        assertThatExceptionOfType(MissingStreamPermissionException.class)
                .isThrownBy(() -> resource.byStreams(req, searchUser))
                .satisfies(ex -> assertThat(ex.streamsWithMissingPermissions()).contains("2323"));
    }

}
