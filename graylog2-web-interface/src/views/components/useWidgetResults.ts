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
import * as Immutable from 'immutable';
import { createSelector } from '@reduxjs/toolkit';

import { widgetDefinition } from 'views/logic/Widgets';
import type Widget from 'views/logic/widgets/Widget';
import type { WidgetMapping } from 'views/logic/views/types';
import type QueryResult from 'views/logic/QueryResult';
import type SearchError from 'views/logic/SearchError';
import useAppSelector from 'stores/useAppSelector';
import { selectSearchExecutionResult } from 'views/logic/slices/searchExecutionSelectors';
import { selectActiveQuery, selectWidget } from 'views/logic/slices/viewSelectors';

const _getDataAndErrors = (widget: Widget, widgetMapping: WidgetMapping, results: QueryResult) => {
  const { searchTypes } = results;
  const widgetType = widgetDefinition(widget.type);
  const dataTransformer = widgetType.searchResultTransformer || ((x) => x);
  const searchTypeIds = (widgetMapping.get(widget.id, Immutable.Set()));
  const widgetData = searchTypeIds.map((searchTypeId) => searchTypes[searchTypeId]).filter((result) => result !== undefined).toArray();
  const widgetErrors = results.errors.filter((e) => searchTypeIds.includes(e.searchTypeId));
  let error;

  const data = dataTransformer(widgetData);

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

const _getErrorsForQuery = (currentQueryId: string, currentQueryErrors: SearchError[] | undefined) => {
  const queryError = currentQueryErrors?.find((searchError) => searchError.queryId === currentQueryId);

  return { widgetData: undefined, error: queryError ? [queryError] : [] };
};

const selectWidgetResults = (widgetId: string) => createSelector(
  selectSearchExecutionResult,
  selectActiveQuery,
  selectWidget(widgetId),
  (searchExecutionResult, currentQueryId, widget) => {
    const { result, widgetMapping } = searchExecutionResult ?? {};
    const currentQueryResults = result?.results?.[currentQueryId];
    const currentQueryErrors = result?.errors;

    return (currentQueryResults
      ? _getDataAndErrors(widget, widgetMapping, currentQueryResults)
      : _getErrorsForQuery(currentQueryId, currentQueryErrors)) as WidgetResults;
  },
);

const useWidgetResults = (widgetId: string) => useAppSelector(selectWidgetResults(widgetId));

export default useWidgetResults;
