package org.graylog.storage.elasticsearch7.views.searchtypes.pivot;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.action.search.SearchResponse;
import org.graylog.shaded.elasticsearch7.org.elasticsearch.search.aggregations.Aggregations;
import org.graylog.storage.elasticsearch7.views.ESGeneratedQueryContext;
import org.graylog.storage.elasticsearch7.views.searchtypes.ESSearchTypeHandler;

import javax.inject.Inject;

public class ESPivotFacade implements ESSearchTypeHandler<Pivot> {
    private final ESPivot esPivot;
    private final ESPivotWithScriptedTerms esPivotWithScriptedTerms;

    @Inject
    public ESPivotFacade(ESPivot esPivot, ESPivotWithScriptedTerms esPivotWithScriptedTerms) {
        this.esPivot = esPivot;
        this.esPivotWithScriptedTerms = esPivotWithScriptedTerms;
    }

    @Override
    public SearchType.Result doExtractResult(SearchJob job, Query query, Pivot pivot, SearchResponse queryResult, Aggregations aggregations, ESGeneratedQueryContext queryContext) {
        return isSingleRowPivot(pivot)
            ? esPivot.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext)
            : esPivotWithScriptedTerms.doExtractResult(job, query, pivot, queryResult, aggregations, queryContext);
    }

    @Override
    public void doGenerateQueryPart(Query query, Pivot pivot, ESGeneratedQueryContext queryContext) {
        if (isSingleRowPivot(pivot)) {
            esPivot.doGenerateQueryPart(query, pivot, queryContext);
        } else {
            esPivotWithScriptedTerms.doGenerateQueryPart(query, pivot, queryContext);
        }

    }

    private boolean isSingleRowPivot(Pivot pivot) {
        return pivot.rowGroups().size() <= 1 && pivot.columnGroups().size() <= 1;
    }
}
