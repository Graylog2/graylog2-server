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
package org.graylog.plugins.views.search.engine;

import com.google.common.base.MoreObjects;
import org.graylog.plugins.views.search.elasticsearch.FieldTypesLookup;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.searchtypes.pivot.Pivot;
import org.graylog.plugins.views.search.searchtypes.pivot.PivotSpec;
import org.graylog.plugins.views.search.searchtypes.pivot.SeriesSpec;
import org.joda.time.DateTimeZone;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public abstract class IndexerGeneratedQueryContext<S> implements GeneratedQueryContext {

    protected final Map<Object, Object> contextMap;
    protected final Set<SearchError> errors;
    protected final FieldTypesLookup fieldTypes;
    protected final DateTimeZone timezone;
    protected final Map<String, S> searchTypeQueries;
    protected final S ssb;

    public IndexerGeneratedQueryContext(final Map<Object, Object> contextMap,
                                        final Set<SearchError> errors,
                                        final FieldTypesLookup fieldTypes,
                                        final DateTimeZone timezone,
                                        final S ssb,
                                        final Map<String, S> searchTypeQueries) {
        this.contextMap = contextMap;
        this.errors = errors;
        this.fieldTypes = fieldTypes;
        this.timezone = timezone;
        this.ssb = ssb;
        this.searchTypeQueries = searchTypeQueries;
    }

    public Optional<String> fieldType(final Set<String> streamIds, final String field) {
        return fieldTypes.getType(streamIds, field);
    }

    public Map<String, S> searchTypeQueries() {
        return this.searchTypeQueries;
    }

    @Override
    public Optional<String> getSearchTypeQueryString(final String id) {
        return Optional.ofNullable(searchTypeQueries.get(id)).map(S::toString);
    }

    @Override
    public void addError(final SearchError error) {
        errors.add(error);
    }

    @Override
    public Collection<SearchError> errors() {
        return errors;
    }

    @Override
    public DateTimeZone timezone() {
        return timezone;
    }

    public Map<Object, Object> contextMap() {
        return contextMap;
    }

    public void recordNameForPivotSpec(final Pivot pivot,
                                       final PivotSpec spec,
                                       final String name) {
        contextMap.putIfAbsent(pivot.id(), new PivotAggsContext());
        final PivotAggsContext pivotAggsContext = (PivotAggsContext) contextMap.get(pivot.id());
        pivotAggsContext.recordNameForPivotSpec(spec, name);
    }

    public String getAggNameForPivotSpecFromContext(final Pivot pivot, final PivotSpec pivotSpec) {
        contextMap.putIfAbsent(pivot.id(), new PivotAggsContext());
        return ((PivotAggsContext) contextMap.get(pivot.id())).getName(pivotSpec);
    }

    public String seriesName(final SeriesSpec seriesSpec, final Pivot pivot) {
        return pivot.id() + "-series-" + seriesSpec.id();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("elasticsearch query", ssb)
                .toString();
    }
}
