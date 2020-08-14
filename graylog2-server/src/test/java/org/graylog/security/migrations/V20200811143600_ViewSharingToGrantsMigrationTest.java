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
package org.graylog.security.migrations;

import org.graylog.grn.GRNRegistry;
import org.graylog.plugins.views.search.views.ViewRequirements;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.DBGrantService;
import org.graylog.security.entities.EntityOwnershipService;
import org.graylog.testing.GRNExtension;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog.testing.mongodb.MongoJackExtension;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MongoJackExtension.class)
@ExtendWith(GRNExtension.class)
@ExtendWith(MockitoExtension.class)
class V20200811143600_ViewSharingToGrantsMigrationTest {
    private V20200811143600_ViewSharingToGrantsMigration migration;

    @BeforeEach
    void setUp(MongoDBTestService mongodb,
               MongoJackObjectMapperProvider objectMapperProvider,
               GRNRegistry grnRegistry,
               @Mock ClusterConfigService clusterConfigService) {

        final DBGrantService grantService = new DBGrantService(mongodb.mongoConnection(), objectMapperProvider, grnRegistry);
        final EntityOwnershipService entityOwnershipService = new EntityOwnershipService(grantService, grnRegistry);
        final TestViewService viewService = new TestViewService(mongodb.mongoConnection(), objectMapperProvider, clusterConfigService, entityOwnershipService);

        this.migration = new V20200811143600_ViewSharingToGrantsMigration(mongodb.mongoConnection(), grantService, viewService);
    }

    @Test
    void upgrade() {
        migration.upgrade();
    }

    public static class TestViewService extends ViewService {
        public TestViewService(MongoConnection mongoConnection,
                               MongoJackObjectMapperProvider mapper,
                               ClusterConfigService clusterConfigService,
                               EntityOwnershipService entityOwnerShipService) {
            super(mongoConnection, mapper, clusterConfigService, view -> new ViewRequirements(Collections.emptySet(), view), entityOwnerShipService);
        }
    }
}
