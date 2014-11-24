/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.name.Names;
import models.ModelFactoryModule;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.api.responses.cluster.NodeSummaryResponse;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class BaseApiTest {
    protected ApiClient api;
    protected ServerNodes serverNodes;
    protected Injector injector;

    public Injector setupGuice(final Collection<URI> initialNodes) {
        List<Module> modules = Lists.newArrayList();
        modules.add(new ModelFactoryModule());

        modules.add(new AbstractModule() {
            @Override
            protected void configure() {
                bind(URI[].class).annotatedWith(Names.named("Initial Nodes")).toInstance(initialNodes.toArray(new URI[initialNodes.size()]));
                bind(Long.class).annotatedWith(Names.named("Default Timeout")).toInstance(TimeUnit.SECONDS.toMillis(5));
            }
        });
        return Guice.createInjector(modules);
    }

    public void registerNodes(ServerNodes serverNodes, Node.Factory factory, AddressNodeId[] nodeDesc) {
        final ArrayList<Node> nodes = Lists.newArrayList();
        for (AddressNodeId n : nodeDesc) {
            NodeSummaryResponse r = new NodeSummaryResponse();
            r.transportAddress = n.address;
            r.nodeId = n.nodeId;
            final Node node = factory.fromSummaryResponse(r);
            node.touch();
            nodes.add(node);
        }
        serverNodes.put(nodes);
    }

    public void setupNodes(AddressNodeId... nodes) {
        final ImmutableList<AddressNodeId> list = ImmutableList.copyOf(nodes);
        final Collection<URI> uris = Collections2.transform(list, new Function<AddressNodeId, URI>() {
            @Nullable
            @Override
            public URI apply(@Nullable AddressNodeId input) {
                assert input != null;
                return input.getUri();
            }
        });
        injector = setupGuice(uris);
        api = injector.getInstance(ApiClient.class);
        serverNodes = injector.getInstance(ServerNodes.class);
        final Node.Factory factory = injector.getInstance(Node.Factory.class);

        registerNodes(serverNodes, factory, nodes);
    }

    public static class AddressNodeId {
        public String address;
        public String nodeId;

        public AddressNodeId(String address, String nodeId) {
            this.address = address;
            this.nodeId = nodeId;
        }

        public URI getUri() {
            return URI.create(address);
        }

        public static AddressNodeId create(String address, String nodeId) {
            return new AddressNodeId(address, nodeId);
        }

        public static AddressNodeId create(String address) {
            return new AddressNodeId(address, UUID.randomUUID().toString());
        }
    }
}
