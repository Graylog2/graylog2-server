/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security.shares;

import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.grn.GRNRegistry;
import org.graylog.grn.GRNTypes;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.rest.PaginationParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
class GranteeSharesServiceTest {
    private GranteeSharesService granteeSharesService;
    private GRNDescriptorService grnDescriptorService;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider mongoJackObjectMapperProvider,
               GRNRegistry grnRegistry,
               @Mock GRNDescriptorService grnDescriptorService) {
        this.grnDescriptorService = grnDescriptorService;
        final DBGrantService dbGrantService = new DBGrantService(mongodb.mongoConnection(), mongoJackObjectMapperProvider, grnRegistry, mock(ClusterEventBus.class));
        this.granteeSharesService = new GranteeSharesService(dbGrantService, grnDescriptorService);
    }

    @DisplayName("Paginated shares for a user")
    @Nested
    @MongoDBFixtures("dashboard-and-stream-shares.json")
    class PaginatedSharesForUser {
        private final PaginationParameters paginationParameters = new PaginationParameters();

        private final GRN stream0 = GRNTypes.STREAM.toGRN("54e3deadbeefdeadbeef0000");
        private final GRN stream1 = GRNTypes.STREAM.toGRN("54e3deadbeefdeadbeef0001");
        private final GRN dashboard0 = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0000");
        private final GRN dashboard1 = GRNTypes.DASHBOARD.toGRN("54e3deadbeefdeadbeef0001");

        @BeforeEach
        void setUp() {
            mockDescriptor(stream0, "Stream 0");
            mockDescriptor(stream1, "Stream 1");
            mockDescriptor(dashboard0, "Dashboard 0");
            mockDescriptor(dashboard1, "Dashboard 1");
        }

        @DisplayName("User: Jane")
        @Nested
        class GetSharesForUserJane {
            @DisplayName("paginated shares")
            @Test
            void getPaginatedSharesForUser() {
                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("jane"), paginationParameters, "", "");

                assertThat(response.paginatedEntities()).hasSize(3);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(dashboard0, "Dashboard 0", Collections.emptySet()));
                assertThat(response.paginatedEntities().get(1)).isEqualTo(EntityDescriptor.create(stream0, "Stream 0", Collections.emptySet()));
                assertThat(response.paginatedEntities().get(2)).isEqualTo(EntityDescriptor.create(stream1, "Stream 1", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(3);
                assertThat(response.capabilities().get(stream0)).isEqualTo(Capability.VIEW);
                assertThat(response.capabilities().get(stream1)).isEqualTo(Capability.MANAGE);
                assertThat(response.capabilities().get(dashboard0)).isEqualTo(Capability.OWN);
                assertThat(response.capabilities()).doesNotContainKey(dashboard1);
            }

            @DisplayName("paginated shares with filter")
            @Test
            void getPaginatedSharesForUserWithFilter() {
                // Only return entities that contain the value in the title
                paginationParameters.setQuery("dashboard");

                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("jane"), paginationParameters, "", "");

                assertThat(response.paginatedEntities()).hasSize(1);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(dashboard0, "Dashboard 0", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(1);
                assertThat(response.capabilities().get(dashboard0)).isEqualTo(Capability.OWN);
                assertThat(response.capabilities()).doesNotContainKey(dashboard1);
                assertThat(response.capabilities()).doesNotContainKey(stream0);
                assertThat(response.capabilities()).doesNotContainKey(stream1);
            }

            @DisplayName("paginated shares with capability filter")
            @Test
            void getPaginatedSharesForUserWithCapabilityFilter() {
                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("jane"), paginationParameters, "viEw", "");

                assertThat(response.paginatedEntities()).hasSize(1);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(stream0, "Stream 0", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(1);
                assertThat(response.capabilities().get(stream0)).isEqualTo(Capability.VIEW);
                assertThat(response.capabilities()).doesNotContainKey(dashboard0);
                assertThat(response.capabilities()).doesNotContainKey(dashboard1);
                assertThat(response.capabilities()).doesNotContainKey(stream1);
            }

            @DisplayName("paginated shares with entity type filter")
            @Test
            void getPaginatedSharesForUserWithEntityTypeFilter() {
                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("jane"), paginationParameters, "", "dashboard");

                assertThat(response.paginatedEntities()).hasSize(1);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(dashboard0, "Dashboard 0", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(1);
                assertThat(response.capabilities().get(dashboard0)).isEqualTo(Capability.OWN);
                assertThat(response.capabilities()).doesNotContainKey(dashboard1);
                assertThat(response.capabilities()).doesNotContainKey(stream0);
                assertThat(response.capabilities()).doesNotContainKey(stream1);
            }

            @DisplayName("paginated shares")
            @Test
            void getPaginatedSharesForUserWithPage() {
                // Only return page two
                paginationParameters.setPerPage(2);
                paginationParameters.setPage(2);

                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("jane"), paginationParameters, "", "");

                assertThat(response.paginatedEntities()).hasSize(1);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(stream1, "Stream 1", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(1);
                assertThat(response.capabilities().get(stream1)).isEqualTo(Capability.MANAGE);
                assertThat(response.capabilities()).doesNotContainKey(dashboard0);
                assertThat(response.capabilities()).doesNotContainKey(dashboard1);
                assertThat(response.capabilities()).doesNotContainKey(stream0);
            }

            @DisplayName("paginated shares with reverse order")
            @Test
            void getPaginatedSharesForUserWithReverseOrder() {
                // Reverse sort order
                paginationParameters.setOrder("desc");

                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("jane"), paginationParameters, "", "");

                assertThat(response.paginatedEntities()).hasSize(3);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(stream1, "Stream 1", Collections.emptySet()));
                assertThat(response.paginatedEntities().get(1)).isEqualTo(EntityDescriptor.create(stream0, "Stream 0", Collections.emptySet()));
                assertThat(response.paginatedEntities().get(2)).isEqualTo(EntityDescriptor.create(dashboard0, "Dashboard 0", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(3);
                assertThat(response.capabilities().get(stream0)).isEqualTo(Capability.VIEW);
                assertThat(response.capabilities().get(stream1)).isEqualTo(Capability.MANAGE);
                assertThat(response.capabilities().get(dashboard0)).isEqualTo(Capability.OWN);
                assertThat(response.capabilities()).doesNotContainKey(dashboard1);
            }
        }

        @DisplayName("User: John")
        @Nested
        class GetSharesForUserJohn {
            @DisplayName("paginated shares")
            @Test
            void getPaginatedSharesForUser() {
                final GranteeSharesService.SharesResponse response = granteeSharesService.getPaginatedSharesFor(GRNTypes.USER.toGRN("john"), paginationParameters, "", "");

                assertThat(response.paginatedEntities()).hasSize(2);
                assertThat(response.paginatedEntities().get(0)).isEqualTo(EntityDescriptor.create(dashboard1, "Dashboard 1", Collections.emptySet()));
                assertThat(response.paginatedEntities().get(1)).isEqualTo(EntityDescriptor.create(stream1, "Stream 1", Collections.emptySet()));

                assertThat(response.capabilities()).hasSize(2);
                assertThat(response.capabilities().get(dashboard1)).isEqualTo(Capability.OWN);
                assertThat(response.capabilities().get(stream1)).isEqualTo(Capability.VIEW);
                assertThat(response.capabilities()).doesNotContainKey(dashboard0);
                assertThat(response.capabilities()).doesNotContainKey(stream0);
            }
        }
    }

    private void mockDescriptor(GRN grn, String title) {
        // Use lenient() so we don't error out if a stub ist not used
        lenient().when(grnDescriptorService.getDescriptor(grn))
                .then(invocation -> GRNDescriptor.create(invocation.getArgument(0, GRN.class), title));
    }
}
