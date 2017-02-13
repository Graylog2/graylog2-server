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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.indexer.IndexSet;
import org.graylog2.indexer.IndexSetRegistry;
import org.graylog2.indexer.cluster.Cluster;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.indices.TooManyAliasesException;
import org.graylog2.rest.models.system.deflector.responses.DeflectorSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexRangeSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexSizeSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexSummary;
import org.graylog2.rest.models.system.indexer.responses.IndexerClusterOverview;
import org.graylog2.rest.models.system.indexer.responses.IndexerOverview;
import org.graylog2.rest.resources.count.CountResource;
import org.graylog2.rest.resources.system.DeflectorResource;
import org.graylog2.rest.resources.system.IndexRangesResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Indexer/Overview", description = "Indexing overview")
@Path("/system/indexer/overview")
public class IndexerOverviewResource extends RestResource {
    private final DeflectorResource deflectorResource;
    private final IndexerClusterResource indexerClusterResource;
    private final IndexRangesResource indexRangesResource;
    private final CountResource countResource;
    private final IndexSetRegistry indexSetRegistry;
    private final Indices indices;
    private final Cluster cluster;

    @Inject
    public IndexerOverviewResource(DeflectorResource deflectorResource,
                                   IndexerClusterResource indexerClusterResource,
                                   IndexRangesResource indexRangesResource,
                                   CountResource countResource,
                                   IndexSetRegistry indexSetRegistry,
                                   Indices indices,
                                   Cluster cluster) {
        this.deflectorResource = deflectorResource;
        this.indexerClusterResource = indexerClusterResource;
        this.indexRangesResource = indexRangesResource;
        this.countResource = countResource;
        this.indexSetRegistry = indexSetRegistry;
        this.indices = indices;
        this.cluster = cluster;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get overview of current indexing state, including deflector config, cluster state, index ranges & message counts.")
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public IndexerOverview index() throws TooManyAliasesException {
        if (!cluster.isConnected()) {
            throw new ServiceUnavailableException("Elasticsearch cluster is not available, check your configuration and logs for more information.");
        }

        try {
            return getIndexerOverview(indexSetRegistry.getDefault());
        } catch (IllegalStateException e) {
            throw new NotFoundException("Default index set not found");
        }
    }

    @GET
    @Timed
    @Path("/{indexSetId}")
    @ApiOperation(value = "Get overview of current indexing state for the given index set, including deflector config, cluster state, index ranges & message counts.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexerOverview index(@ApiParam(name = "indexSetId") @PathParam("indexSetId") String indexSetId) throws TooManyAliasesException {
        if (!cluster.isConnected()) {
            throw new ServiceUnavailableException("Elasticsearch cluster is not available, check your configuration and logs for more information.");
        }

        final IndexSet indexSet = getIndexSet(indexSetRegistry, indexSetId);

        return getIndexerOverview(indexSet);
    }

    private IndexerOverview getIndexerOverview(IndexSet indexSet) throws TooManyAliasesException {
        final String indexSetId = indexSet.getConfig().id();

        final DeflectorSummary deflectorSummary = deflectorResource.deflector(indexSetId);
        final List<IndexRangeSummary> indexRanges = indexRangesResource.list().ranges();
        final Map<String, IndexStats> allDocCounts = indices.getAllDocCounts(indexSet).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        final Map<String, Boolean> areReopened = indices.areReopened(allDocCounts.keySet());
        final Map<String, IndexSummary> indicesSummaries = allDocCounts.values()
                .stream()
                .parallel()
                .collect(Collectors.toMap(IndexStats::getIndex,
                        (indexStats) -> IndexSummary.create(
                                IndexSizeSummary.create(indexStats.getPrimaries().getDocs().getCount(),
                                        indexStats.getPrimaries().getDocs().getDeleted(),
                                        indexStats.getPrimaries().getStore().sizeInBytes()),
                                indexRanges.stream().filter((indexRangeSummary) -> indexRangeSummary.indexName().equals(indexStats.getIndex())).findFirst().orElse(null),
                                deflectorSummary.currentTarget() != null && deflectorSummary.currentTarget().equals(indexStats.getIndex()),
                                false,
                                areReopened.get(indexStats.getIndex()))
                ));

        indices.getClosedIndices(indexSet).forEach(indexName -> indicesSummaries.put(indexName, IndexSummary.create(
                null,
                indexRanges.stream().filter((indexRangeSummary) -> indexRangeSummary.indexName().equals(indexName)).findFirst().orElse(null),
                deflectorSummary.currentTarget() != null && deflectorSummary.currentTarget().equals(indexName),
                true,
                false
        )));

        return IndexerOverview.create(deflectorSummary,
                IndexerClusterOverview.create(indexerClusterResource.clusterHealth(), indexerClusterResource.clusterName().name()),
                countResource.total(indexSetId),indicesSummaries);
    }

}
