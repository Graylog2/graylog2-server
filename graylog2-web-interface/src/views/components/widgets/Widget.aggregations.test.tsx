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
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';

import MockStore from 'helpers/mocking/StoreMock';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetModel from 'views/logic/widgets/Widget';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import View from 'views/logic/views/View';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import DataTable from 'views/components/datatable';
import DataTableVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/DataTableVisualizationConfig';
import { asMock } from 'helpers/mocking';
import useViewType from 'views/hooks/useViewType';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import { updateWidget } from 'views/logic/slices/widgetActions';

import Widget from './Widget';
import type { Props as WidgetComponentProps } from './Widget';

import WidgetContext from '../contexts/WidgetContext';
import WidgetFocusContext from '../contexts/WidgetFocusContext';
import FieldTypesContext from '../contexts/FieldTypesContext';

const testTimeout = applyTimeoutMultiplier(60000);
const mockedUnixTime = 1577836800000; // 2020-01-01 00:00:00.000

jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('./WidgetColorContext', () => ({ children }) => children);
jest.mock('views/logic/fieldtypes/useFieldTypes');

jest.mock('views/hooks/useAggregationFunctions');

jest.mock('views/stores/StreamsStore', () => ({
  StreamsStore: MockStore(['getInitialState', () => ({
    streams: [
      { title: 'Stream 1', id: 'stream-id-1' },
    ],
  })]),
}));

jest.mock('views/hooks/useViewType');

jest.mock('views/hooks/useAutoRefresh', () => () => ({
  refreshConfig: null,
  startAutoRefresh: () => {},
  stopAutoRefresh: () => {},
}));

jest.mock('views/logic/slices/widgetActions', () => ({
  ...jest.requireActual('views/logic/slices/widgetActions'),
  updateWidget: jest.fn(() => async () => {}),
}));

const selectEventConfig = { container: document.body };

describe('Aggregation Widget', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  const dataTableWidget = WidgetModel.builder().newId()
    .type('AGGREGATION')
    .config(AggregationWidgetConfig.builder().visualization(DataTable.type).visualizationConfig(DataTableVisualizationConfig.create([]).toBuilder().build()).build())
    .query(createElasticsearchQueryString(''))
    .timerange({ type: 'relative', from: 300 })
    .build();

  beforeEach(() => {
    jest.useFakeTimers()
      .setSystemTime(mockedUnixTime);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  type AggregationWidgetProps = Partial<WidgetComponentProps>;

  const widgetFocusContextState = {
    focusedWidget: undefined,
    setWidgetFocusing: () => {},
    setWidgetEditing: () => {},
    unsetWidgetFocusing: () => {},
    unsetWidgetEditing: () => {},
  };

  const AggregationWidget = ({
    widget: propsWidget = dataTableWidget,
    ...props
  }: AggregationWidgetProps) => (
    <TestStoreProvider>
      <FieldTypesContext.Provider value={{ all: Immutable.List(), queryFields: Immutable.Map() }}>
        <WidgetFocusContext.Provider value={widgetFocusContextState}>
          <WidgetContext.Provider value={propsWidget}>
            <Widget widget={propsWidget}
                    id="widgetId"
                    onPositionsChange={() => {}}
                    title="Widget Title"
                    position={new WidgetPosition(1, 1, 1, 1)}
                    {...props} />
          </WidgetContext.Provider>
        </WidgetFocusContext.Provider>
      </FieldTypesContext.Provider>
    </TestStoreProvider>
  );

  const findWidgetConfigSubmitButton = () => screen.findByRole('button', { name: /update preview/i });

  const submitWidgetChanges = () => {
    const saveButton = screen.getByRole('button', { name: /update widget/i });
    fireEvent.click(saveButton);
  };

  describe('on a dashboard', () => {
    beforeEach(() => {
      asMock(useViewType).mockReturnValue(View.Type.Dashboard);
    });

    it('should apply not submitted widget search controls and aggregation elements changes when clicking on "Update widget"', async () => {
      const newSeries = Series.create('count').toBuilder().config(SeriesConfig.empty().toBuilder().name('Metric name').build()).build();
      const updatedConfig = dataTableWidget.config
        .toBuilder()
        .series([newSeries])
        .build();

      const updatedWidget = dataTableWidget.toBuilder()
        .config(updatedConfig)
        .streams(['stream-id-1'])
        .build();
      render(<AggregationWidget editing />);

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

      submitWidgetChanges();

      await waitFor(() => expect(updateWidget).toHaveBeenCalledWith(expect.any(String), updatedWidget));
    }, testTimeout);

    it('should apply not submitted widget time range changes in correct format when clicking on "Update widget"', async () => {
      // Displayed times are based on time zone defined in moment-timezone mock.
      const updatedWidget = dataTableWidget
        .toBuilder()
        .timerange({
          from: '2019-12-31T23:55:00.000+00:00',
          to: '2020-01-01T00:00:00.000+00:00',
          type: 'absolute',
        })
        .build();

      render(<AggregationWidget editing />);

      // Change widget time range
      const timeRangePickerButton = await screen.findByLabelText('Open Time Range Selector');
      userEvent.click(timeRangePickerButton);

      const absoluteTabButton = await screen.findByRole('tab', { name: /absolute/i });
      jest.setSystemTime(mockedUnixTime);
      userEvent.click(absoluteTabButton);

      const applyTimeRangeChangesButton = await screen.findByRole('button', { name: 'Update time range' });
      userEvent.click(applyTimeRangeChangesButton);

      const timeRangeDisplay = await screen.findByLabelText('Search Time Range, Opens Time Range Selector On Click');
      await within(timeRangeDisplay).findByText('2020-01-01 00:55:00.000');

      // Submit all changes
      submitWidgetChanges();

      await waitFor(() => expect(updateWidget).toHaveBeenCalledWith(expect.any(String), updatedWidget));
    }, testTimeout);
  });

  describe('on a search', () => {
    it('should apply not submitted aggregation elements changes when clicking on "Update widget"', async () => {
      const newSeries = Series.create('count').toBuilder().config(SeriesConfig.empty().toBuilder().name('Metric name').build()).build();
      const updatedConfig = dataTableWidget.config
        .toBuilder()
        .series([newSeries])
        .build();

      const updatedWidget = dataTableWidget.toBuilder()
        .config(updatedConfig)
        .build();
      render(<AggregationWidget editing />);

      // Change widget aggregation elements
      const addMetricButton = await screen.findByRole('button', { name: 'Add a Metric' });
      fireEvent.click(addMetricButton);

      const nameInput = await screen.findByLabelText(/Name/);
      userEvent.type(nameInput, 'Metric name');

      const metricFieldSelect = screen.getByLabelText('Select a function');
      await selectEvent.openMenu(metricFieldSelect);
      await selectEvent.select(metricFieldSelect, 'Count', selectEventConfig);

      await findWidgetConfigSubmitButton();

      submitWidgetChanges();

      await waitFor(() => expect(updateWidget).toHaveBeenCalledWith(expect.any(String), updatedWidget));
    }, testTimeout);
  });
});
