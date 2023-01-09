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
import type Widget from 'views/logic/widgets/Widget';
import type { WidgetMapping } from 'views/logic/views/types';
import type QueryResult from 'views/logic/QueryResult';
import type SearchError from 'views/logic/SearchError';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import useWidget from 'views/hooks/useWidget';
import useAppSelector from 'stores/useAppSelector';
import { selectSearchExecutionResult } from 'views/logic/slices/searchExecutionSlice';

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

const useWidgetResults = (widgetId: string) => {
  const currentQueryId = useActiveQueryId();

  const widget = useWidget(widgetId);

  const searchExecutionResult = useAppSelector(selectSearchExecutionResult);

  const widgetResults = useMemo(() => {
    const { result, widgetMapping } = searchExecutionResult ?? {};
    const currentQueryResults = result?.results?.[currentQueryId];

    return (currentQueryResults
      ? _getDataAndErrors(widget, widgetMapping, currentQueryResults)
      : { widgetData: undefined, error: [] });
  }, [currentQueryId, searchExecutionResult, widget]);

  return widgetResults as WidgetResults;
};

export default useWidgetResults;
