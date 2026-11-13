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
import * as React from 'react';
import * as Immutable from 'immutable';
import { render } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import { AssistantPageContextProvider, useAssistantPageSnapshot } from 'components/assistant';
import type { PageContext, PageTools, SearchPage } from 'components/assistant';
import useViewsSelector from 'views/stores/useViewsSelector';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { selectActiveQuery, selectView } from 'views/logic/slices/viewSelectors';
import { selectGlobalOverride } from 'views/logic/slices/searchExecutionSelectors';
import { setGlobalOverrideQuery } from 'views/logic/slices/searchExecutionSlice';
import { executeActiveQuery, updateQueryString, updateViewState } from 'views/logic/slices/viewSlice';
import GlobalOverride from 'views/logic/search/GlobalOverride';
import View from 'views/logic/views/View';
import ViewState from 'views/logic/views/ViewState';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import Direction from 'views/logic/aggregationbuilder/Direction';
import BarVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/BarVisualizationConfig';
import VisualizationConfig from 'views/logic/aggregationbuilder/visualizations/VisualizationConfig';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import { addWidget } from 'views/logic/slices/widgetActions';
import { createSearch } from 'fixtures/searches';
import type Widget from 'views/logic/widgets/Widget';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import TextWidget from 'views/logic/widgets/TextWidget';
import EventsWidget from 'views/logic/widgets/events/EventsWidget';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import type { FocusContextState } from 'views/components/contexts/WidgetFocusContext';

import SearchPageAssistantContext from './SearchPageAssistantContext';

jest.mock('routing/useLocation', () => () => ({ pathname: '/search' }));
jest.mock('views/stores/useViewsSelector');
jest.mock('views/stores/useViewsDispatch');
jest.mock('views/logic/slices/widgetActions', () => ({ addWidget: jest.fn(() => 'addWidget') }));
jest.mock('views/logic/slices/searchExecutionSlice', () => ({
  ...jest.requireActual('views/logic/slices/searchExecutionSlice'),
  setGlobalOverrideQuery: jest.fn(() => 'setGlobalOverrideQuery'),
}));
jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  updateQueryString: jest.fn(() => 'updateQueryString'),
  updateViewState: jest.fn(() => 'updateViewState'),
  executeActiveQuery: jest.fn(() => 'executeActiveQuery'),
}));

const renderPage = (focusedWidget?: FocusContextState) => {
  let snapshot: ReturnType<typeof useAssistantPageSnapshot>;
  const Snapshot = () => {
    snapshot = useAssistantPageSnapshot();

    return null;
  };

  render(
    <WidgetFocusContext.Provider
      value={{
        focusedWidget,
        setWidgetFocusing: jest.fn(),
        unsetWidgetFocusing: jest.fn(),
        setWidgetEditing: jest.fn(),
        unsetWidgetEditing: jest.fn(),
      }}>
      <AssistantPageContextProvider>
        <SearchPageAssistantContext />
        <Snapshot />
      </AssistantPageContextProvider>
    </WidgetFocusContext.Provider>,
  );

  return { getTools: (): PageTools => snapshot.getTools(), getPage: (): PageContext['page'] => snapshot.getSnapshot().page };
};

const renderTools = () => renderPage().getTools;

const addedWidget = () => asMock(addWidget).mock.calls[0][0] as Widget;

const barChart = AggregationWidget.builder()
  .id('w1')
  .config(
    AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source'])])
      .series([Series.forFunction('count()')])
      .sort([new SortConfig(SortConfig.PIVOT_TYPE, 'source', Direction.Descending)])
      .visualization('bar')
      .build(),
  )
  .build();

// Runs thunks against a page whose shown tab has the bar chart.
const mockPageWithBarChart = (widget: Widget = barChart) => {
  const viewState = ViewState.create()
    .toBuilder()
    .widgets(Immutable.List([widget]))
    .titles({ widget: { w1: 'Logins per host' } })
    .build();
  const state = {
    view: {
      activeQuery: 'query-id',
      view: createSearch()
        .toBuilder()
        .state(Immutable.Map({ 'query-id': viewState }))
        .build(),
    },
  };
  asMock(useViewsDispatch).mockReturnValue(
    jest.fn((action: unknown) =>
      typeof action === 'function' ? action(jest.fn(), () => state) : Promise.resolve(),
    ) as unknown as ReturnType<typeof useViewsDispatch>,
  );
};

const updatedViewState = () => asMock(updateViewState).mock.calls[0][1] as ViewState;

describe('SearchPageAssistantContext', () => {
  const mockState = (view: View, globalOverride?: GlobalOverride) =>
    asMock(useViewsSelector).mockImplementation((selector) => {
      if (selector === selectView) return view;
      if (selector === selectActiveQuery) return 'query-id';
      if (selector === selectGlobalOverride) return globalOverride;

      return undefined;
    });

  beforeAll(() => {
    VisualizationConfig.registerSubtype('bar', BarVisualizationConfig);
  });

  beforeEach(() => {
    jest.clearAllMocks();
    mockState(createSearch());
    asMock(useViewsDispatch).mockReturnValue(jest.fn(async () => {}) as unknown as ReturnType<typeof useViewsDispatch>);
  });

  it('adds a message table with the given columns, sort and search', async () => {
    const tools = renderTools();

    await tools().add_message_table({
      title: 'Failed logins',
      query: 'action:login_failed',
      fields: ['timestamp', 'user_name'],
      sort: { field: 'user_name', direction: 'Ascending' },
    });

    const widget = addedWidget() as MessagesWidget;

    expect(widget).toBeInstanceOf(MessagesWidget);
    expect(widget.config.fields).toEqual(['timestamp', 'user_name']);
    expect(widget.config.sort[0].field).toBe('user_name');
    expect(widget.config.sort[0].direction.direction).toBe('Ascending');
    expect(widget.query).toEqual({ type: 'elasticsearch', query_string: 'action:login_failed' });
    // Added with its title in one change, so a single undo removes both.
    expect(addWidget).toHaveBeenCalledWith(widget, undefined, 'Failed logins');
  });

  it('adds a message table with the default columns', async () => {
    const tools = renderTools();

    await tools().add_message_table({ title: 'Messages' });

    expect((addedWidget() as MessagesWidget).config.fields).toEqual(['timestamp', 'source']);
  });

  it('adds a text widget', async () => {
    const tools = renderTools();

    const result = await tools().add_text_widget({ title: 'Notes', text: '# Hey there!' });

    expect(addedWidget()).toBeInstanceOf(TextWidget);
    expect(addedWidget().config.text).toBe('# Hey there!');
    expect(result).toEqual(expect.objectContaining({ summary: 'Added "Notes"' }));
  });

  it('adds an events widget with its mode and filters', async () => {
    const tools = renderTools();

    await tools().add_events_widget({
      title: 'Critical alerts',
      mode: 'Number',
      filters: [{ field: 'priority', value: ['4'] }],
    });

    const widget = addedWidget() as EventsWidget;

    expect(widget).toBeInstanceOf(EventsWidget);
    expect(widget.config.mode).toBe('Number');
    expect(widget.config.filters.toArray()).toEqual([{ field: 'priority', value: ['4'] }]);
  });

  it('turns a bar chart into a line chart, keeping its sort, in one change', async () => {
    mockPageWithBarChart();
    const tools = renderTools();

    const result = await tools().update_widget({
      widget_id: 'w1',
      title: 'Logins over time',
      config: {
        row_pivots: [{ fields: ['timestamp'], type: 'time', config: { interval: { type: 'auto', scaling: 1.0 } } }],
        column_pivots: [{ fields: ['source'], type: 'values', config: { limit: 15 } }],
        series: [{ function: 'count()', config: {} }],
        sort: [],
        visualization: 'line',
        rollup: true,
      },
    });

    expect(updateViewState).toHaveBeenCalledTimes(1);
    const widget = updatedViewState().widgets.first() as AggregationWidget;

    expect(widget.id).toBe('w1');
    expect(widget.config.visualization).toBe('line');
    expect(widget.config.rowPivots.map(({ fields }) => fields)).toEqual([['timestamp']]);
    expect(widget.config.sort.map(({ field }) => field)).toEqual(['source']);
    expect(updatedViewState().titles.getIn(['widget', 'w1'])).toBe('Logins over time');
    expect(result).toEqual(expect.objectContaining({ summary: 'Changed "Logins over time"' }));
  });

  it("keeps the user's group-by limits, metric names and axis settings when changing a chart", async () => {
    mockPageWithBarChart(
      barChart
        .toBuilder()
        .config(
          barChart.config
            .toBuilder()
            .rowPivots([Pivot.createValues(['source'], { limit: 5, skip_empty_values: true })])
            .series([Series.fromJSON({ function: 'count()', config: { name: 'Logins', thresholds: [] } })])
            .visualizationConfig(BarVisualizationConfig.create('group', 'logarithmic'))
            .build(),
        )
        .build(),
    );
    const tools = renderTools();

    await tools().update_widget({
      widget_id: 'w1',
      config: {
        row_pivots: [{ fields: ['source'], type: 'values', config: { limit: 15 } }],
        column_pivots: [],
        series: [{ function: 'count()', config: {} }, { function: 'avg(took_ms)', config: {} }],
        sort: [],
        visualization: 'bar',
        visualization_config: { barmode: 'stack' },
        rollup: true,
      },
    });

    const { config } = updatedViewState().widgets.first() as AggregationWidget;

    expect(config.rowPivots[0].config).toEqual({ limit: 5, skip_empty_values: true });
    expect(config.series.map(({ effectiveName }) => effectiveName)).toEqual(['Logins', 'avg(took_ms)']);
    expect(config.visualizationConfig).toEqual(BarVisualizationConfig.create('stack', 'logarithmic'));
  });

  it('changes only the search of a widget when no aggregation is given', async () => {
    mockPageWithBarChart();
    const tools = renderTools();

    await tools().update_widget({ widget_id: 'w1', query: 'action:login_failed' });

    const widget = updatedViewState().widgets.first() as AggregationWidget;

    expect(widget.query).toEqual({ type: 'elasticsearch', query_string: 'action:login_failed' });
    expect(widget.config.visualization).toBe('bar');
    expect(updatedViewState().titles.getIn(['widget', 'w1'])).toBe('Logins per host');
  });

  it('refuses to change a widget that is not on the shown tab', async () => {
    mockPageWithBarChart();
    const tools = renderTools();

    await expect(tools().update_widget({ widget_id: 'gone', title: 'x' })).rejects.toThrow('not on the shown tab');
    expect(updateViewState).not.toHaveBeenCalled();
  });

  it('sets the query of the shown tab of a search', async () => {
    mockState(createSearch().toBuilder().type(View.Type.Search).build());
    const tools = renderTools();

    await tools().set_query({ query: 'action:login_failed' });

    expect(updateQueryString).toHaveBeenCalledWith('query-id', 'action:login_failed');
    expect(setGlobalOverrideQuery).not.toHaveBeenCalled();
    expect(executeActiveQuery).toHaveBeenCalled();
  });

  it('sets the query of the search bar of a dashboard, and restores it on undo', async () => {
    const dashboard = createSearch().toBuilder().type(View.Type.Dashboard).build();
    mockState(dashboard, GlobalOverride.create(undefined, createElasticsearchQueryString('source:web-03')));
    const tools = renderTools();

    const result = await tools().set_query({ query: 'action:login_failed' });

    expect(setGlobalOverrideQuery).toHaveBeenCalledWith('action:login_failed');
    expect(updateQueryString).not.toHaveBeenCalled();
    expect(executeActiveQuery).toHaveBeenCalledTimes(1);

    await (result as { undo: () => Promise<void> }).undo();

    expect(setGlobalOverrideQuery).toHaveBeenLastCalledWith('source:web-03');
    expect(executeActiveQuery).toHaveBeenCalledTimes(2);
  });

  it('tells the Assistant whether the focused widget is being edited', () => {
    const { getPage } = renderPage({ id: 'w1', editing: true, focusing: true });

    expect(getPage()).toEqual(expect.objectContaining({ focused_widget_id: 'w1', focused_widget_editing: true }));
  });

  it('tells the Assistant about a focused widget that is not being edited', () => {
    const { getPage } = renderPage({ id: 'w1', editing: false, focusing: true });

    expect((getPage() as SearchPage).focused_widget_editing).toBe(false);
  });
});
