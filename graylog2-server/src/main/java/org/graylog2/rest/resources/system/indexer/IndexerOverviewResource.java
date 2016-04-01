package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.elasticsearch.action.admin.indices.stats.IndexStats;
import org.graylog2.indexer.indices.Indices;
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
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
    private final Indices indices;

    @Inject
    public IndexerOverviewResource(DeflectorResource deflectorResource,
                                   IndexerClusterResource indexerClusterResource,
                                   IndexRangesResource indexRangesResource,
                                   CountResource countResource,
                                   Indices indices) {
        this.deflectorResource = deflectorResource;
        this.indexerClusterResource = indexerClusterResource;
        this.indexRangesResource = indexRangesResource;
        this.countResource = countResource;
        this.indices = indices;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get overview of current indexing state, including deflector config, cluster state, index rnages & message counts.")
    @Produces(MediaType.APPLICATION_JSON)
    public IndexerOverview index() throws ClassNotFoundException {
        final DeflectorSummary deflectorSummary = deflectorResource.deflector();
        final List<IndexRangeSummary> indexRanges = indexRangesResource.list().ranges();
        final Map<String, IndexSummary> indicesSummaries = indices.getAll().values()
            .stream()
            .collect(Collectors.toMap(IndexStats::getIndex,
                (indexStats) -> IndexSummary.create(
                    IndexSizeSummary.create(indexStats.getPrimaries().getDocs().getCount(),
                        indexStats.getPrimaries().getDocs().getDeleted(),
                        indexStats.getPrimaries().getStore().sizeInBytes()),
                    indexRanges.stream().filter((indexRangeSummary) -> indexRangeSummary.indexName().equals(indexStats.getIndex())).findFirst().orElse(null),
                    deflectorSummary.currentTarget().equals(indexStats.getIndex()),
                    false,
                    indices.isReopened(indexStats.getIndex()))
            ));

        indices.getClosedIndices().stream().forEach((indexName) -> {
            indicesSummaries.put(indexName, IndexSummary.create(
                null,
                indexRanges.stream().filter((indexRangeSummary) -> indexRangeSummary.indexName().equals(indexName)).findFirst().orElse(null),
                deflectorSummary.currentTarget().equals(indexName),
                true,
                indices.isReopened(indexName)
            ));
        });

        return IndexerOverview.create(deflectorSummary,
            IndexerClusterOverview.create(indexerClusterResource.clusterHealth(), indexerClusterResource.clusterName().name()),
            countResource.total(),indicesSummaries);
    }
}
