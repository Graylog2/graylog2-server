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
import { useContext, useMemo } from 'react';

import { useAssistantPageContext, useAssistantPageTools } from 'components/assistant';
import type { PageTools, SearchPage } from 'components/assistant';
import useViewsSelector from 'views/stores/useViewsSelector';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { selectActiveQuery, selectIsDirty, selectView } from 'views/logic/slices/viewSelectors';
import { selectGlobalOverride, selectParameterBindings } from 'views/logic/slices/searchExecutionSelectors';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import View from 'views/logic/views/View';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { AggregationWidgetConfigJson } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import { addWidget } from 'views/logic/slices/widgetActions';
import { setTitle } from 'views/logic/slices/titlesActions';
import TitleTypes from 'views/stores/TitleTypes';
import { executeActiveQuery, setTimerange, updateQueryString } from 'views/logic/slices/viewSlice';
import { setGlobalOverrideTimerange } from 'views/logic/slices/searchExecutionSlice';
import { undo } from 'views/logic/slices/undoRedoActions';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import type { TimeRange } from 'views/logic/queries/Query';

const useSearchPage = (): SearchPage => {
  const view = useViewsSelector(selectView);
  const activeQueryId = useViewsSelector(selectActiveQuery);
  const dirty = useViewsSelector(selectIsDirty);
  const globalOverride = useViewsSelector(selectGlobalOverride);
  const parameterBindings = useViewsSelector(selectParameterBindings);
  const { focusedWidget } = useContext(WidgetFocusContext);

  return useMemo(
    () => ({
      type: 'search' as const,
      // The whole view as the browser holds it; the backend loads the persisted search behind it.
      view: JSON.parse(JSON.stringify(view)),
      dirty,
      active_query_id: activeQueryId,
      overrides: {
        query: globalOverride?.query?.query_string,
        timerange: globalOverride?.timerange,
        parameter_values: parameterBindings?.toJS?.() ?? undefined,
      },
      focused_widget_id: focusedWidget?.id,
    }),
    [activeQueryId, dirty, focusedWidget?.id, globalOverride, parameterBindings, view],
  );
};

type AddWidgetArgs = {
  title: string;
  config: AggregationWidgetConfigJson;
  query?: string;
  streams?: Array<string>;
  timerange?: TimeRange;
};

const useSearchPageTools = (): PageTools => {
  const dispatch = useViewsDispatch();
  const view = useViewsSelector(selectView);
  const activeQueryId = useViewsSelector(selectActiveQuery);
  const isDashboard = view?.type === View.Type.Dashboard;

  return useMemo(
    () => ({
      add_widget: async (args) => {
        const { title, config, query, streams, timerange } = args as AddWidgetArgs;
        let builder = AggregationWidget.builder().newId().config(AggregationWidgetConfig.fromJSON(config));

        if (query !== undefined) {
          builder = builder.query(createElasticsearchQueryString(query));
        }

        if (streams?.length) {
          builder = builder.streams(streams);
        }

        if (timerange) {
          builder = builder.timerange(timerange);
        }

        const widget = builder.build();
        await dispatch(addWidget(widget));
        await dispatch(setTitle(activeQueryId, TitleTypes.Widget, widget.id, title));

        return { summary: `Added "${title}"`, undo: () => dispatch(undo()) };
      },
      set_query: async (args) => {
        const { query } = args as { query: string };
        await dispatch(updateQueryString(activeQueryId, query));

        if (!isDashboard) {
          await dispatch(executeActiveQuery());
        }

        return { summary: `Changed the query to "${query}"`, undo: () => dispatch(undo()) };
      },
      set_timerange: async (args) => {
        const { timerange } = args as { timerange: TimeRange };
        if (isDashboard) {
          await dispatch(setGlobalOverrideTimerange(timerange));
        } else {
          await dispatch(setTimerange(activeQueryId, timerange));
        }

        await dispatch(executeActiveQuery());

        return { summary: 'Changed the time range', undo: isDashboard ? undefined : () => dispatch(undo()) };
      },
    }),
    [activeQueryId, dispatch, isDashboard],
  );
};

// Tells the Assistant what the search or dashboard shows, and lets it change the page.
const SearchPageAssistantContext = () => {
  useAssistantPageContext(useSearchPage());
  useAssistantPageTools(useSearchPageTools());

  return null;
};

export default SearchPageAssistantContext;
