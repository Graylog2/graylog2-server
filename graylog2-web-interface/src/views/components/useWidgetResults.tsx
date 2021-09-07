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
import { useMemo } from 'react';
import * as Immutable from 'immutable';

import { widgetDefinition } from 'views/logic/Widgets';
import { useStore } from 'stores/connect';
import { SearchStore } from 'views/stores/SearchStore';
import { WidgetStore } from 'views/stores/WidgetStore';
import Widget from 'views/logic/widgets/Widget';
import { WidgetMapping } from 'views/logic/views/types';
import QueryResult from 'views/logic/QueryResult';
import { ViewStore } from 'views/stores/ViewStore';
import SearchError from 'views/logic/SearchError';

const _getDataAndErrors = (widget: Widget, widgetMapping: WidgetMapping, results: QueryResult) => {
  const { searchTypes } = results;
  const widgetType = widgetDefinition(widget.type);
  const dataTransformer = widgetType.searchResultTransformer || ((x) => x);
  const searchTypeIds = (widgetMapping.get(widget.id, Immutable.Set()));
  const widgetData = searchTypeIds.map((searchTypeId) => searchTypes[searchTypeId]).filter((result) => result !== undefined).toArray();
  const widgetErrors = results.errors.filter((e) => searchTypeIds.includes(e.searchTypeId));
  let error;

  const data = dataTransformer(widgetData, widget);

  if (widgetErrors && widgetErrors.length > 0) {
    error = widgetErrors;
  }

  if (!widgetData || widgetData.length === 0) {
    const queryErrors = results.errors.filter((e) => e.type === 'query');

    if (queryErrors.length > 0) {
      error = error ? [].concat(error, queryErrors) : queryErrors;
    }
  }

  return { widgetData: data, error };
};

type WidgetResults = {
  widgetData: unknown | undefined,
  error: SearchError[],
};

const useWidgetResults = (widgetId: string) => {
  const { widgetMapping, results } = useStore(SearchStore, ({ result: r, widgetMapping: w }) => ({ results: r, widgetMapping: w }));
  const widget = useStore(WidgetStore, (widgets) => widgets.get(widgetId));
  const currentQueryId = useStore(ViewStore, ({ activeQuery }) => activeQuery);

  const widgetResults = useMemo(() => {
    const currentQueryResults = results?.forId(currentQueryId);

    return (currentQueryResults
      ? _getDataAndErrors(widget, widgetMapping, currentQueryResults)
      : { widgetData: undefined, error: [] });
  }, [currentQueryId, results, widget, widgetMapping]);

  return widgetResults as WidgetResults;
};

export default useWidgetResults;
