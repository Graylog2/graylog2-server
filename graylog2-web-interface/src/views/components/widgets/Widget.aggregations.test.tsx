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
import { render, waitFor, fireEvent, screen } from 'wrappedTestingLibrary';
import mockAction from 'helpers/mocking/MockAction';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import MockStore from 'helpers/mocking/StoreMock';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';
import { applyTimeoutMultiplier } from 'jest-preset-graylog/lib/timeouts';
import { createSearch } from 'fixtures/searches';

import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetModel from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import View, { ViewType } from 'views/logic/views/View';
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

jest.mock('./WidgetHeader', () => 'widget-header');
jest.mock('./WidgetColorContext', () => ({ children }) => children);

jest.mock('views/stores/FieldTypesStore', () => ({
  FieldTypesStore: MockStore(['getInitialState', () => ({ all: {}, queryFields: {} })]),
}));

jest.mock('views/stores/WidgetStore', () => ({
  WidgetStore: MockStore(),
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

const viewsPlugin = new PluginManifest({}, viewsBindings);

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
    ViewStore.getInitialState = jest.fn(() => viewStoreState);
  });

  afterEach(() => {
    jest.clearAllMocks();
    jest.resetModules();
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
      <FieldTypesContext.Provider value={{ all: Immutable.List(), queryFields: Immutable.Map() }}>
        <WidgetFocusContext.Provider value={widgetFocusContextState}>
          <WidgetContext.Provider value={propsWidget}>
            <Widget widget={propsWidget}
                    id="widgetId"
                    fields={Immutable.List([])}
                    onPositionsChange={() => {}}
                    onSizeChange={() => {}}
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
    it('should update not submitted widget search controls and aggregation elements changes when clicking on "Apply Changes"', async () => {
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

      const metricFieldSelect = screen.getByLabelText('Select a function');
      await selectEvent.openMenu(metricFieldSelect);
      await selectEvent.select(metricFieldSelect, 'Count');

      await findWidgetConfigSubmitButton();

      // Change widget search controls
      const streamsSelect = await screen.findByLabelText('Select streams the search should include. Searches in all streams if empty.');
      await selectEvent.openMenu(streamsSelect);
      await selectEvent.select(streamsSelect, 'Stream 1');

      await screen.findByRole('button', {
        name: /perform search \(changes were made after last search execution\)/i,
      });

      // Submit all changes
      const saveButton = screen.getByText('Apply Changes');
      fireEvent.click(saveButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledTimes(1));

      expect(WidgetActions.update).toHaveBeenCalledWith(expect.any(String), updatedWidget);
    }, testTimeout);
  });

  describe('on a search', () => {
    it('should update not submitted aggregation elements changes when clicking on "Apply Changes"', async () => {
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
      await selectEvent.select(metricFieldSelect, 'Count');

      await findWidgetConfigSubmitButton();

      // Submit all changes
      const saveButton = screen.getByText('Apply Changes');
      fireEvent.click(saveButton);

      await waitFor(() => expect(WidgetActions.update).toHaveBeenCalledTimes(1));

      expect(WidgetActions.update).toHaveBeenCalledWith(expect.any(String), updatedWidget);
    }, testTimeout);
  });
});
