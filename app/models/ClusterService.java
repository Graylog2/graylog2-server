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

package models;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import lib.APIException;
import lib.ApiClient;
import models.api.responses.system.ESClusterHealthResponse;
import models.api.responses.system.ServerJVMStatsResponse;
import models.api.responses.system.ServerThroughputResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ClusterService {
    private static final Logger log = LoggerFactory.getLogger(ClusterService.class);
    private final ApiClient api;

    @Inject
    private ClusterService(ApiClient api) {
        this.api = api;
    }

    public ESClusterHealth getESClusterHealth() {
        try {
            final ESClusterHealthResponse response = api.get(ESClusterHealthResponse.class).path("/system/indexer/cluster/health").execute();
            return new ESClusterHealth(response);
        } catch (APIException e) {
            log.error("Could not load es cluster health", e);
        } catch (IOException e) {
            log.error("Could not load es cluster health", e);
        }
        return null;
    }

    public List<ServerJVMStats> getClusterJvmStats() {
        List<ServerJVMStats> result = Lists.newArrayList();
        Collection<ServerJVMStatsResponse> rs = api.get(ServerJVMStatsResponse.class).fromAllNodes().path("/system/jvm").executeOnAll();

        for (ServerJVMStatsResponse r : rs) {
            result.add(new ServerJVMStats(r));
        }

        return result;
    }

    public int getClusterThroughput() {
        final Collection<ServerThroughputResponse> responses =
                api.get(ServerThroughputResponse.class).fromAllNodes().path("/system/throughput").executeOnAll();
        int t = 0;
        for (ServerThroughputResponse r : responses) {
            t += r.throughput;
        }
        return t;
    }
}
