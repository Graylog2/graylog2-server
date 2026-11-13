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
import { OrderedSet } from 'immutable';
import isEqual from 'lodash/isEqual';

import { useAssistantPageContext, useAssistantPageTools } from 'components/assistant';
import type { PageTools, SearchPage } from 'components/assistant';
import useViewsSelector from 'views/stores/useViewsSelector';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { GetState } from 'views/types';
import { selectActiveQuery, selectActiveViewState, selectIsDirty, selectView } from 'views/logic/slices/viewSelectors';
import { selectGlobalOverride, selectParameterBindings } from 'views/logic/slices/searchExecutionSelectors';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import View from 'views/logic/views/View';
import type Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { AggregationWidgetConfigJson } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { VisualizationConfigJson } from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import type Pivot from 'views/logic/aggregationbuilder/Pivot';
import type Series from 'views/logic/aggregationbuilder/Series';
import Direction from 'views/logic/aggregationbuilder/Direction';
import type { DirectionJson } from 'views/logic/aggregationbuilder/Direction';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import MessagesWidgetConfig from 'views/logic/widgets/MessagesWidgetConfig';
import TextWidget from 'views/logic/widgets/TextWidget';
import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import EventsWidget from 'views/logic/widgets/events/EventsWidget';
import EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import type { Filter, VisualizationType } from 'views/logic/widgets/events/EventsWidgetConfig';
import { DEFAULT_MESSAGE_FIELDS } from 'views/Constants';
import { addWidget } from 'views/logic/slices/widgetActions';
import TitleTypes from 'views/stores/TitleTypes';
import { executeActiveQuery, setTimerange, updateQueryString, updateViewState } from 'views/logic/slices/viewSlice';
import normalizeViewState from 'views/logic/views/normalizeViewState';
import { setGlobalOverrideQuery, setGlobalOverrideTimerange } from 'views/logic/slices/searchExecutionSlice';
import { undo } from 'views/logic/slices/undoRedoActions';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import Store from 'logic/local-storage/Store';
import generateId from 'logic/generateId';
import Routes from 'routing/Routes';
import type { EventDefinitionLocalStorageConfig } from 'components/event-definitions/hooks/useEventDefinitionConfigFromLocalStorage';
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
      focused_widget_editing: !!focusedWidget?.editing,
    }),
    [activeQueryId, dirty, focusedWidget?.editing, focusedWidget?.id, globalOverride, parameterBindings, view],
  );
};

type WidgetSearchArgs = {
  title: string;
  query?: string;
  streams?: Array<string>;
  timerange?: TimeRange;
};

type AddWidgetArgs = WidgetSearchArgs & { config: AggregationWidgetConfigJson };

type UpdateWidgetArgs = Partial<WidgetSearchArgs> & { widget_id: string; config?: AggregationWidgetConfigJson };

type AddMessageTableArgs = WidgetSearchArgs & {
  fields?: Array<string>;
  sort?: { field: string; direction: DirectionJson };
};

type AddEventsWidgetArgs = WidgetSearchArgs & { mode: VisualizationType; filters: Array<Filter> };

type CreateEventDefinitionArgs = {
  title: string;
  description: string;
  priority: number;
  query: string;
  streams: Array<string>;
  search_within_ms: number;
  execute_every_ms: number;
  condition?: {
    function: string;
    field: string;
    operator: EventDefinitionLocalStorageConfig['agg_operator'];
    threshold: number;
    group_by: Array<string>;
  };
};

// Hands the event definition to its form, as creating one from a widget's value does. Runs synchronously, so the
// browser lets the click on the proposal open the new tab.
const openEventDefinitionForm = (args: Record<string, unknown>) => {
  const { condition, ...definition } = args as CreateEventDefinitionArgs;
  const sessionId = `assistant-${generateId()}`;
  const config: EventDefinitionLocalStorageConfig = {
    type: 'aggregation-v1',
    ...definition,
    ...(condition
      ? {
          agg_function: condition.function,
          agg_field: condition.field || undefined,
          agg_operator: condition.operator,
          agg_value: condition.threshold,
          group_by: condition.group_by,
        }
      : {}),
  };
  Store.set(sessionId, config);

  if (!window.open(`${Routes.ALERTS.DEFINITIONS.CREATE}?step=event-details&session-id=${sessionId}`, '_blank')) {
    Store.delete(sessionId);
    throw new Error('The browser blocked the new tab for the event definition.');
  }

  return { summary: `Opened the form for event definition "${definition.title}" in a new tab` };
};

type WidgetBuilder = ReturnType<typeof Widget.builder>;

// Without them, the widget uses the query, streams and time range of the page.
const withSearch = (builder: WidgetBuilder, { query, streams, timerange }: Partial<WidgetSearchArgs>) => {
  let result = builder;

  if (query !== undefined) {
    result = result.query(createElasticsearchQueryString(query));
  }

  if (streams?.length) {
    result = result.streams(streams);
  }

  if (timerange) {
    result = result.timerange(timerange);
  }

  return result;
};

const visualizationConfigJson = (config: AggregationWidgetConfig) =>
  (config.visualizationConfig as { toJSON?: () => Record<string, unknown> } | undefined)?.toJSON?.() ?? {};

// The assistant sends only the visualization settings it changes, e.g. the bar mode; the others stay as they are.
const withCurrentVisualizationConfig = (
  current: AggregationWidgetConfig,
  changed: AggregationWidgetConfigJson,
): AggregationWidgetConfigJson =>
  changed.visualization === current.visualization && current.visualizationConfig
    ? {
        ...changed,
        visualization_config: {
          ...visualizationConfigJson(current),
          ...(changed.visualization_config ?? {}),
        } as VisualizationConfigJson,
      }
    : changed;

// The changed aggregation, with the settings the user chose for the widget where they still apply: the limits and
// intervals of its group-bys, the names of its metrics, its sort, and its visualization's settings.
const mergeAggregation = (current: AggregationWidgetConfig, changedJson: AggregationWidgetConfigJson) => {
  const changed = AggregationWidgetConfig.fromJSON(withCurrentVisualizationConfig(current, changedJson));
  const currentPivots = [...current.rowPivots, ...current.columnPivots];
  const keepPivot = (pivot: Pivot) =>
    currentPivots.find(({ type, fields }) => type === pivot.type && isEqual(fields, pivot.fields)) ?? pivot;
  const keepSeries = (series: Series) =>
    current.series.find(({ function: fn }) => fn === series.function) ?? series;
  const fields = new Set([...changed.rowPivots, ...changed.columnPivots].flatMap((pivot) => pivot.fields));
  const functions = new Set(changed.series.map((series) => series.function));
  const sort = current.sort.filter(({ type, field }) =>
    type === SortConfig.PIVOT_TYPE ? fields.has(field) : functions.has(field),
  );
  const sameVisualization = changed.visualization === current.visualization;

  return changed
    .toBuilder()
    .rowPivots(changed.rowPivots.map(keepPivot))
    .columnPivots(changed.columnPivots.map(keepPivot))
    .series(changed.series.map(keepSeries))
    .sort(sort)
    .rollup(sameVisualization ? current.rollup : changed.rollup)
    .visualizationConfig(changed.visualizationConfig ?? (sameVisualization ? current.visualizationConfig : undefined))
    .formattingSettings(current.formattingSettings)
    .units(current.units)
    .eventAnnotation(current.eventAnnotation)
    .build();
};

const useSearchPageTools = (): PageTools => {
  const dispatch = useViewsDispatch();
  const view = useViewsSelector(selectView);
  const activeQueryId = useViewsSelector(selectActiveQuery);
  const globalOverride = useViewsSelector(selectGlobalOverride);
  const isDashboard = view?.type === View.Type.Dashboard;

  return useMemo(() => {
    const add = async (widget: Widget, title: string) => {
      await dispatch(addWidget(widget, undefined, title));

      return { summary: `Added "${title}"`, undo: () => dispatch(undo()) };
    };

    return {
      add_widget: async (args) => {
        const { config, ...search } = args as AddWidgetArgs;
        const builder = AggregationWidget.builder().newId().config(AggregationWidgetConfig.fromJSON(config));

        return add(withSearch(builder, search).build(), search.title);
      },
      add_message_table: async (args) => {
        const { fields, sort, ...search } = args as AddMessageTableArgs;
        let config = MessagesWidgetConfig.builder()
          .fields(fields ?? DEFAULT_MESSAGE_FIELDS)
          .showMessageRow(true)
          .showSummary(true);

        if (sort) {
          config = config.sort([new SortConfig(SortConfig.PIVOT_TYPE, sort.field, Direction.fromJSON(sort.direction))]);
        }

        return add(withSearch(MessagesWidget.builder().newId().config(config.build()), search).build(), search.title);
      },
      update_widget: async (args) => {
        const { widget_id: widgetId, title, config, ...search } = args as UpdateWidgetArgs;
        const viewState = dispatch((_dispatch: ViewsDispatch, getState: GetState) => selectActiveViewState(getState()));
        const widget = viewState?.widgets.find(({ id }) => id === widgetId);

        if (!widget) {
          throw new Error('The widget is not on the shown tab anymore.');
        }

        let builder = withSearch(widget.toBuilder(), search);

        if (config) {
          if (!(widget instanceof AggregationWidget)) {
            throw new Error('Only aggregation widgets have a visualization, group-bys and metrics.');
          }

          builder = builder.config(mergeAggregation(widget.config, config));
        }

        // One change, so a single undo reverts the widget and its title.
        const updated = builder.build();
        const titles = title ? viewState.titles.setIn([TitleTypes.Widget, widgetId], title) : viewState.titles;
        const newViewState = viewState
          .toBuilder()
          .widgets(viewState.widgets.map((current) => (current.id === widgetId ? updated : current)).toList())
          .titles(titles)
          .build();
        await dispatch(updateViewState(activeQueryId, normalizeViewState(newViewState)));
        const shownTitle = title ?? viewState.titles.getIn([TitleTypes.Widget, widgetId]);

        return {
          summary: shownTitle ? `Changed "${shownTitle}"` : 'Changed the widget',
          undo: () => dispatch(undo()),
        };
      },
      add_text_widget: async (args) => {
        const { title, text } = args as { title: string; text: string };

        return add(TextWidget.builder().newId().config(new TextWidgetConfig(text)).build(), title);
      },
      add_events_widget: async (args) => {
        const { mode, filters, ...search } = args as AddEventsWidgetArgs;
        const config = EventsWidgetConfig.createDefault().toBuilder().mode(mode).filters(OrderedSet(filters)).build();

        return add(withSearch(EventsWidget.builder().newId().config(config), search).build(), search.title);
      },
      set_query: async (args) => {
        const { query } = args as { query: string };
        const summary = `Changed the query to "${query}"`;

        // A dashboard's search bar sets the query of all its widgets, like its time range.
        if (isDashboard) {
          const previousQuery = globalOverride?.query?.query_string ?? '';
          const setQuery = async (queryString: string) => {
            await dispatch(setGlobalOverrideQuery(queryString));
            await dispatch(executeActiveQuery());
          };
          await setQuery(query);

          return { summary, undo: () => setQuery(previousQuery) };
        }

        await dispatch(updateQueryString(activeQueryId, query));
        await dispatch(executeActiveQuery());

        return { summary, undo: () => dispatch(undo()) };
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
      create_event_definition: openEventDefinitionForm,
    };
  }, [activeQueryId, dispatch, globalOverride, isDashboard]);
};

// Tells the Assistant what the search or dashboard shows, and lets it change the page.
const SearchPageAssistantContext = () => {
  useAssistantPageContext(useSearchPage());
  useAssistantPageTools(useSearchPageTools());

  return null;
};

export default SearchPageAssistantContext;
