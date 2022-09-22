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
import React from 'react';
import * as Immutable from 'immutable';
import { render, waitFor, fireEvent, screen, within } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import MockStore from 'helpers/mocking/StoreMock';
import mockAction from 'helpers/mocking/MockAction';
import { createSearch } from 'fixtures/searches';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetModel from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { ViewType } from 'views/logic/views/View';
import View from 'views/logic/views/View';
import { ViewStore } from 'views/stores/ViewStore';
import type { ViewStoreState } from 'views/stores/ViewStore';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import viewsBindings from 'views/bindings';
import DataTable from 'views/components/datatable/DataTable';

import Widget from './Widget';
import type { Props as WidgetComponentProps } from './Widget';

import WidgetContext from '../contexts/WidgetContext';
import WidgetFocusContext from '../contexts/WidgetFocusContext';
import FieldTypesContext from '../contexts/FieldTypesContext';
import ViewTypeContext from '../contexts/ViewTypeContext';

const testTimeout = applyTimeoutMultiplier(30000);
const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('./WidgetColorContext', () => ({ children }) => children);
jest.mock('views/logic/fieldtypes/useFieldTypes');

const MockWidgetStoreState = Immutable.Map();

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: MockStore(['getInitialState', () => MockWidgetStoreState]),
  WidgetActions: {
    update: mockAction(),
  },
}));

jest.mock('views/stores/AggregationFunctionsStore', () => ({
  getInitialState: jest.fn(() => ({
    count: { type: 'count', description: 'Count' },
  })),
  listen: jest.fn(),
}));

jest.mock('views/stores/StreamsStore', () => ({
  StreamsStore: MockStore(['getInitialState', () => ({
    streams: [
      { title: 'Stream 1', id: 'stream-id-1' },
    ],
  })]),
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: MockStore(['getInitialState', () => ({ search: { parameters: [] } })]),
  SearchActions: {
    execute: mockAction(),
  },
}));

const viewsPlugin = new PluginManifest({}, viewsBindings);

const selectEventConfig = { container: document.body };

describe('Aggregation Widget', () => {
  beforeAll(() => {
    PluginStore.register(viewsPlugin);
  });

  afterAll(() => {
    PluginStore.unregister(viewsPlugin);
  });

  const dataTableWidget = WidgetModel.builder().newId()
    .type('AGGREGATION')
    .config(AggregationWidgetConfig.builder().visualization(DataTable.type).build())
    .query(createElasticsearchQueryString(''))
    .timerange({ type: 'relative', from: 300 })
    .build();

  const viewStoreState: ViewStoreState = {
    activeQuery: 'query-id-1',
    view: createSearch({ queryId: 'query-id-1' }),
    isNew: false,
    dirty: false,
  };

  beforeEach(() => {
    jest.useFakeTimers()
      // @ts-expect-error
      .setSystemTime(mockedUnixTime);

    ViewStore.getInitialState = jest.fn(() => viewStoreState);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
    jest.useRealTimers();
  });

  type AggregationWidgetProps = Partial<WidgetComponentProps> & {
    viewType: ViewType,
  }

  const widgetFocusContextState = {
    focusedWidget: undefined,
    setWidgetFocusing: () => {},
    setWidgetEditing: () => {},
    unsetWidgetFocusing: () => {},
    unsetWidgetEditing: () => {},
  };

  const AggregationWidget = ({
    widget: propsWidget = dataTableWidget,
    viewType,
    ...props
  }: AggregationWidgetProps) => (
    <ViewTypeContext.Provider value={viewType}>
      {/* eslint-disable-next-line react/jsx-no-constructed-context-values */}
      <FieldTypesContext.Provider value={{ all: Immutable.List(), queryFields: Immutable.Map() }}>
        <WidgetFocusContext.Provider value={widgetFocusContextState}>
          <WidgetContext.Provider value={propsWidget}>
            <Widget widget={propsWidget}
                    id="widgetId"
                    fields={Immutable.List([])}
                    onPositionsChange={() => {}}
                    title="Widget Title"
                    position={new WidgetPosition(1, 1, 1, 1)}
                    {...props} />
          </WidgetContext.Provider>
        </WidgetFocusContext.Provider>
      </FieldTypesContext.Provider>
    </ViewTypeContext.Provider>
  );

  const findWidgetConfigSubmitButton = () => screen.findByRole('button', { name: 'Update Preview' });

  describe('on a dashboard', () => {
    it('should apply not submitted widget search controls and aggregation elements changes when clicking on "Apply Changes"', async () => {
      const newSeries = Series.create('count').toBuilder().config(SeriesConfig.empty().toBuilder().name('Metric name').build()).build();
      const updatedConfig = dataTableWidget.config
        .toBuilder()
        .series([newSeries])
        .build();

      const updatedWidget = dataTableWidget.toBuilder()
        .config(updatedConfig)
        .streams(['stream-id-1'])
        .build();
      render(<AggregationWidget editing viewType={View.Type.Dashboard} />);

      // Change widget aggregation elements
      const addMetricButton = await screen.findByRole('button', { name: 'Add a Metric' });
      fireEvent.click(addMetricButton);

      const nameInput = await screen.findByLabelText(/Name/);
      userEvent.type(nameInput, 'Metric name');

      const metricFieldSelect = await screen.findByLabelText('Select a function');
      await selectEvent.openMenu(metricFieldSelect);
      await selectEvent.select(metricFieldSelect, 'Count', selectEventConfig);

      await findWidgetConfigSubmitButton();

      // Change widget search controls
      const streamsSelect = await screen.findByLabelText('Select streams the search should include. Searches in all streams if empty.');
      await selectEvent.openMenu(streamsSelect);
      await selectEvent.select(streamsSelect, 'Stream 1', selectEventConfig);

      await screen.findByRole('button', {
        name: /perform search \(changes were made after last search execution\)/i,
      });

      // Submit all changes
      const saveButton = screen.getByText('Apply Changes');
      fireEvent.click(saveButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledTimes(1));

      expect(WidgetActions.update).toHaveBeenCalledWith(expect.any(String), updatedWidget);
    }, testTimeout);

    it('should apply not submitted widget time range changes in correct format when clicking on "Apply Changes"', async () => {
      // Displayed times are based on time zone defined in moment-timezone mock.
      const updatedWidget = dataTableWidget
        .toBuilder()
        .timerange({
          from: '2019-12-31T23:55:00.000+00:00',
          to: '2020-01-01T00:00:00.000+00:00',
          type: 'absolute',
        })
        .build();

      render(<AggregationWidget editing viewType={View.Type.Dashboard} />);

      // Change widget time range
      const timeRangeDropdownButton = await screen.findByLabelText('Open Time Range Selector');
      userEvent.click(timeRangeDropdownButton);

      const absoluteTabButton = await screen.findByRole('tab', { name: /absolute/i });
      userEvent.click(absoluteTabButton);

      const timeRangeLivePreview = await screen.findByTestId('time-range-live-preview');
      await within(timeRangeLivePreview).findByText('2020-01-01 00:55:00.000');

      const applyTimeRangeChangesButton = await screen.findByRole('button', { name: 'Apply' });
      userEvent.click(applyTimeRangeChangesButton);

      const timeRangeDisplay = await screen.findByLabelText('Search Time Range, Opens Time Range Selector On Click');
      await within(timeRangeDisplay).findByText('2020-01-01 00:55:00.000');

      // Submit all changes
      const saveButton = screen.getByText('Apply Changes');
      fireEvent.click(saveButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledTimes(1));

      expect(WidgetActions.update).toHaveBeenCalledWith(expect.any(String), updatedWidget);
    }, testTimeout);
  });

  describe('on a search', () => {
    it('should apply not submitted aggregation elements changes when clicking on "Apply Changes"', async () => {
      const newSeries = Series.create('count').toBuilder().config(SeriesConfig.empty().toBuilder().name('Metric name').build()).build();
      const updatedConfig = dataTableWidget.config
        .toBuilder()
        .series([newSeries])
        .build();

      const updatedWidget = dataTableWidget.toBuilder()
        .config(updatedConfig)
        .build();
      render(<AggregationWidget editing viewType={View.Type.Dashboard} />);

      // Change widget aggregation elements
      const addMetricButton = await screen.findByRole('button', { name: 'Add a Metric' });
      fireEvent.click(addMetricButton);

      const nameInput = await screen.findByLabelText(/Name/);
      userEvent.type(nameInput, 'Metric name');

      const metricFieldSelect = screen.getByLabelText('Select a function');
      await selectEvent.openMenu(metricFieldSelect);
      await selectEvent.select(metricFieldSelect, 'Count', selectEventConfig);

      await findWidgetConfigSubmitButton();

      // Submit all changes
      const saveButton = screen.getByText('Apply Changes');
      fireEvent.click(saveButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledTimes(1));

      expect(WidgetActions.update).toHaveBeenCalledWith(expect.any(String), updatedWidget);
    }, testTimeout);
  });
});
