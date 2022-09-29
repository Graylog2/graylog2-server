package org.graylog.storage.opensearch2.views.searchtypes.pivot;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.shaded.opensearch2.org.opensearch.action.search.SearchResponse;
import org.graylog.shaded.opensearch2.org.opensearch.search.aggregations.Aggregations;
import org.graylog.storage.opensearch2.views.OSGeneratedQueryContext;
import org.graylog.storage.opensearch2.views.searchtypes.OSSearchTypeHandler;

import javax.inject.Inject;

public class OSPivotFacade implements OSSearchTypeHandler<Pivot> {
    private final OSPivot osPivot;
    private final OSPivotWithLinearBuckets osPivotWithLinearBuckets;

    @Inject
    public OSPivotFacade(OSPivot osPivot,
                         OSPivotWithLinearBuckets osPivotWithLinearBuckets) {
        this.osPivot = osPivot;
        this.osPivotWithLinearBuckets = osPivotWithLinearBuckets;
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, OSGeneratedQueryContext queryContext) {
        return isSingleRowPivot(pivot)
                ? osPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext)
                : osPivotWithLinearBuckets.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, OSGeneratedQueryContext queryContext) {
        if (isSingleRowPivot(pivot)) {
            osPivot.doGenerateQueryPart(query, pivot, queryContext);
            return;
        }

        osPivotWithLinearBuckets.doGenerateQueryPart(query, pivot, queryContext);
    }

    private boolean isSingleRowPivot(Pivot pivot) {
        return pivot.rowGroups().size() <= 1 && pivot.columnGroups().size() <= 1;
    }
}
