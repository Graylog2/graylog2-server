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
package org.graylog2.cluster;

import com.lordofthejars.nosqlunit.annotation.UsingDataSet;
import com.lordofthejars.nosqlunit.core.LoadStrategyEnum;
import com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb;
import com.mongodb.DBCollection;
import org.graylog2.Configuration;
import org.graylog2.database.MongoConnectionRule;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.URI;

import static com.lordofthejars.nosqlunit.mongodb.InMemoryMongoDb.InMemoryMongoRuleBuilder.newInMemoryMongoDbRule;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeServiceImplTest {
    @ClassRule
    public static final InMemoryMongoDb IN_MEMORY_MONGO_DB = newInMemoryMongoDbRule().build();

    private static final URI TRANSPORT_URI = URI.create("http://10.0.0.1:12900");
    private static final String LOCAL_CANONICAL_HOSTNAME = Tools.getLocalCanonicalHostname();
    private static final String NODE_ID = "28164cbe-4ad9-4c9c-a76e-088655aa7889";

    @Rule
    public MongoConnectionRule mongoRule = MongoConnectionRule.build("test");

    @Mock
    private Configuration configuration;
    @Mock
    private NodeId nodeId;

    private NodeService nodeService;

    @Before
    public void setUp() throws Exception {
        when(nodeId.toString()).thenReturn(NODE_ID);

        this.nodeService = new NodeServiceImpl(mongoRule.getMongoConnection(), configuration);
    }

    @Test
    @UsingDataSet(locations = "NodeServiceImplTest-empty.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testRegisterServer() throws Exception {
        assertThat(nodeService.allActive())
                .describedAs("The collection should be empty")
                .isEmpty();

        nodeService.registerServer(nodeId.toString(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);

        final Node node = nodeService.byNodeId(nodeId);

        assertThat(node).isNotNull();
        assertThat(node.getHostname()).isEqualTo(LOCAL_CANONICAL_HOSTNAME);
        assertThat(node.getTransportAddress()).isEqualTo(TRANSPORT_URI.toString());
        assertThat(node.isMaster()).isTrue();
    }

    @Test
    @UsingDataSet(locations = "NodeServiceImplTest-one-node.json", loadStrategy = LoadStrategyEnum.CLEAN_INSERT)
    public void testRegisterServerWithExistingNode() throws Exception {
        final Node node1 = nodeService.byNodeId(nodeId);

        assertThat(node1.getNodeId())
                .describedAs("There should be one existing node")
                .isEqualTo(NODE_ID);

        nodeService.registerServer(nodeId.toString(), true, TRANSPORT_URI, LOCAL_CANONICAL_HOSTNAME);

        final DBCollection collection = mongoRule.getMongoConnection().getDatabase().getCollection("nodes");

        assertThat(collection.count())
                .describedAs("There should only be one node")
                .isEqualTo(1);

        final Node node2 = nodeService.byNodeId(nodeId);

        assertThat(node2).isNotNull();
        assertThat(node2.getHostname()).isEqualTo(LOCAL_CANONICAL_HOSTNAME);
        assertThat(node2.getTransportAddress()).isEqualTo(TRANSPORT_URI.toString());
        assertThat(node2.isMaster()).isTrue();
    }
}