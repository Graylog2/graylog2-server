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
import { List, OrderedSet } from 'immutable';
import isEqual from 'lodash/isEqual';
import isPlainObject from 'lodash/isPlainObject';

import { useAssistantPageContext, useAssistantPageTools } from 'components/assistant';
import type { PageTools, SearchPage } from 'components/assistant';
import useViewsSelector from 'views/stores/useViewsSelector';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import type { ViewsDispatch } from 'views/stores/useViewsDispatch';
import type { FiltersType, GetState, SearchFilter } from 'views/types';
import { selectActiveQuery, selectActiveViewState, selectIsDirty, selectView } from 'views/logic/slices/viewSelectors';
import { selectGlobalOverride, selectParameterBindings } from 'views/logic/slices/searchExecutionSelectors';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import View from 'views/logic/views/View';
import type Widget from 'views/logic/widgets/Widget';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { AggregationWidgetConfigJson } from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
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
import {
  executeActiveQuery,
  setTimerange,
  updateQuery,
  updateQueryString,
  updateViewState,
} from 'views/logic/slices/viewSlice';
import normalizeViewState from 'views/logic/views/normalizeViewState';
import { setGlobalOverrideQuery, setGlobalOverrideTimerange } from 'views/logic/slices/searchExecutionSlice';
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
      focused_widget_editing: !!focusedWidget?.editing,
      active_query_filters: view?.search?.queries?.find(({ id }) => id === activeQueryId)?.filters?.toArray(),
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

type Settings = Record<string, unknown>;

type UpdateWidgetArgs = Partial<WidgetSearchArgs> & {
  widget_id: string;
  config?: AggregationWidgetConfigJson;
  // The settings of the visualization the config switches to, for those its changes do not give.
  visualization_defaults?: Settings;
  // Changed settings of the widget's visualization, which must still be the given one, without changing its aggregation.
  visualization?: string;
  visualization_config?: Settings;
};

type AddMessageTableArgs = WidgetSearchArgs & {
  fields?: Array<string>;
  sort?: { field: string; direction: DirectionJson };
};

type AddEventsWidgetArgs = WidgetSearchArgs & { mode: VisualizationType; filters: Array<Filter> };

type WidgetBuilder = ReturnType<typeof Widget.builder>;

type SearchFilterArgs = {
  filter_id: string;
  title?: string;
  query_string?: string;
  description?: string;
  negation?: boolean;
  widget_id?: string;
};

const isFilter = (id: string) => (filter: SearchFilter) => filter.type === 'referenced' && filter.id === id;

const withoutFilter = (filters: FiltersType, id: string) => filters.filterNot(isFilter(id)).toList();

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

const visualizationConfigJson = (config: AggregationWidgetConfig): Settings =>
  (config.visualizationConfig as { toJSON?: () => Settings } | undefined)?.toJSON?.() ?? {};

// Changed settings replace the current ones, nested ones key by key, e.g. the label of one axis; null resets one.
const mergeSettings = (current: Settings, changes: Settings): Settings =>
  Object.entries(changes).reduce((merged, [key, value]) => {
    if (value === null) {
      const { [key]: _reset, ...rest } = merged;

      return rest;
    }

    const previous = merged[key];

    return {
      ...merged,
      [key]:
        isPlainObject(value) && isPlainObject(previous)
          ? mergeSettings(previous as Settings, value as Settings)
          : value,
    };
  }, current);

// The assistant sends only the visualization settings it changes; the others stay as they are, or, for another
// visualization, become its defaults.
const withCurrentVisualizationConfig = (
  current: AggregationWidgetConfig,
  changed: AggregationWidgetConfigJson,
  defaults: Settings | undefined,
): AggregationWidgetConfigJson => {
  const changes = changed.visualization_config as Settings | undefined;

  if (changed.visualization === current.visualization && current.visualizationConfig) {
    return {
      ...changed,
      visualization_config: mergeSettings(visualizationConfigJson(current), changes ?? {}) as VisualizationConfigJson,
    };
  }

  return changes
    ? { ...changed, visualization_config: mergeSettings(defaults ?? {}, changes) as VisualizationConfigJson }
    : changed;
};

const withChangedSettings = (current: AggregationWidgetConfig, visualization: string, changes: Settings) => {
  if (visualization && current.visualization !== visualization) {
    throw new Error(`The widget shows a ${current.visualization} visualization now, not ${visualization}.`);
  }

  return current
    .toBuilder()
    .visualizationConfig(
      VisualizationConfig.fromJSON(current.visualization, mergeSettings(visualizationConfigJson(current), changes)),
    )
    .build();
};

// The changed aggregation, with the settings the user chose for the widget where they still apply: the limits and
// intervals of its group-bys, the names of its metrics, its sort, and its visualization's settings.
const mergeAggregation = (
  current: AggregationWidgetConfig,
  changedJson: AggregationWidgetConfigJson,
  defaults: Settings | undefined,
) => {
  const changed = AggregationWidgetConfig.fromJSON(withCurrentVisualizationConfig(current, changedJson, defaults));
  const currentPivots = [...current.rowPivots, ...current.columnPivots];
  const keepPivot = (pivot: Pivot) =>
    currentPivots.find(({ type, fields }) => type === pivot.type && isEqual(fields, pivot.fields)) ?? pivot;
  const keepSeries = (series: Series) => current.series.find(({ function: fn }) => fn === series.function) ?? series;
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

    // Changes the search filters of a widget, or of the shown query of a search, in one change the user can undo.
    const changeFilters = async (widgetId: string | undefined, change: (filters: FiltersType) => FiltersType) => {
      if (widgetId) {
        const viewState = dispatch((_dispatch: ViewsDispatch, getState: GetState) => selectActiveViewState(getState()));
        const widget = viewState?.widgets.find(({ id }) => id === widgetId);

        if (!widget) {
          throw new Error('The widget is not on the shown tab anymore.');
        }

        const updated = widget
          .toBuilder()
          .filters(change(widget.filters ?? List()))
          .build();
        const widgets = viewState.widgets.map((current) => (current.id === widgetId ? updated : current)).toList();
        await dispatch(
          updateViewState(activeQueryId, normalizeViewState(viewState.toBuilder().widgets(widgets).build())),
        );
      } else {
        if (isDashboard) {
          throw new Error('A dashboard has no search filters of its own; give the widget to filter.');
        }

        const query = dispatch((_dispatch: ViewsDispatch, getState: GetState) =>
          selectView(getState()).search.queries.find(({ id }) => id === activeQueryId),
        );
        await dispatch(
          updateQuery(
            activeQueryId,
            query
              .toBuilder()
              .filters(change(query.filters ?? List()))
              .build(),
          ),
        );
      }

      await dispatch(executeActiveQuery());

      return () => dispatch(undo());
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
        const {
          widget_id: widgetId,
          title,
          config,
          visualization_defaults: defaults,
          visualization,
          visualization_config: settings,
          ...search
        } = args as UpdateWidgetArgs;
        const viewState = dispatch((_dispatch: ViewsDispatch, getState: GetState) => selectActiveViewState(getState()));
        const widget = viewState?.widgets.find(({ id }) => id === widgetId);

        if (!widget) {
          throw new Error('The widget is not on the shown tab anymore.');
        }

        let builder = withSearch(widget.toBuilder(), search);

        if (config || settings) {
          if (!(widget instanceof AggregationWidget)) {
            throw new Error('Only aggregation widgets have a visualization, group-bys and metrics.');
          }

          builder = builder.config(
            config
              ? mergeAggregation(widget.config, config, defaults)
              : withChangedSettings(widget.config, visualization, settings),
          );
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
      add_search_filter: async (args) => {
        const { filter_id: id, title, query_string, description, negation, widget_id } = args as SearchFilterArgs;
        const filter: SearchFilter = {
          type: 'referenced',
          id,
          title,
          queryString: query_string ?? '',
          description,
          negation: !!negation,
          disabled: false,
        };
        const undoChange = await changeFilters(widget_id, (filters) => withoutFilter(filters, id).push(filter));

        return {
          summary: `Added search filter "${title ?? id}"${negation ? ', excluding its matches' : ''}`,
          undo: undoChange,
        };
      },
      remove_search_filter: async (args) => {
        const { filter_id: id, title, widget_id } = args as SearchFilterArgs;
        const undoChange = await changeFilters(widget_id, (filters) => {
          if (!filters.some(isFilter(id))) {
            throw new Error(`The search filter "${title ?? id}" is not applied there.`);
          }

          return withoutFilter(filters, id);
        });

        return { summary: `Removed search filter "${title ?? id}"`, undo: undoChange };
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
