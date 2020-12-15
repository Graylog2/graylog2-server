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
import { render, fireEvent, waitFor } from 'wrappedTestingLibrary';
import { StoreMock as MockStore } from 'helpers/mocking';
import asMock from 'helpers/mocking/AsMock';

import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import Store from 'logic/local-storage/Store';
import SearchActions from 'views/actions/SearchActions';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';
import ViewState from 'views/logic/views/ViewState';

import { mockFieldCharts, viewState as mockViewState } from './MigrateFieldCharts.fixtures';

import MigrateFieldCharts from '../MigrateFieldCharts';

const mockStoreGet = (fieldChart = {}, migrated = false) => (key) => {
  switch (key) {
    case 'pinned-field-charts-migrated':
      return migrated;
    case 'pinned-field-charts':
      return mockFieldCharts(fieldChart);
    default:
      return undefined;
  }
};

jest.mock('logic/local-storage/Store', () => ({
  get: jest.fn(),
  set: jest.fn(),
}));

jest.mock('views/stores/ViewStatesStore', () => ({
  ViewStatesActions: {
    update: jest.fn(() => Promise.resolve()),
  },
}));

jest.mock('views/actions/SearchActions', () => ({
  executeWithCurrentState: jest.fn(() => Promise.resolve()),
}));

jest.mock('views/stores/CurrentViewStateStore', () => ({
  CurrentViewStateStore: MockStore(
    ['getInitialState', () => {
      return {
        state: mockViewState(),
        activeQuery: 'active-query-id',
      };
    },
    ],
  ),
}));

jest.mock('views/logic/Widgets', () => ({
  widgetDefinition: () => ({
    defaultHeight: 4,
    defaultWidth: 4,
  }),
}));

const getNewWidget = (actionMock) => {
  const viewState = asMock(actionMock).mock.calls[0][1] as ViewState;
  const widgetsTotal = viewState.widgets.size;

  return viewState.widgets.get(widgetsTotal - 1);
};

const getNewWidgetConfig = (actionMock) => {
  return getNewWidget(actionMock).config;
};

const renderAndMigrate = () => {
  const { queryByText, getByText } = render(<MigrateFieldCharts />);
  const migrateButton = getByText('Migrate');

  fireEvent.click(migrateButton);

  return { queryByText };
};

describe('MigrateFieldCharts', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should be visible if migration never got executed', () => {
    Store.get.mockImplementation(mockStoreGet());
    const { getByText } = render(<MigrateFieldCharts />);

    expect(getByText('Migrate existing search page charts')).not.toBeNull();
  });

  it('should not be visible if migration already got executed', () => {
    Store.get.mockImplementation(mockStoreGet(undefined, true));
    const { queryByText } = render(<MigrateFieldCharts />);

    expect(queryByText('Migrate existing search page charts')).toBeNull();
  });

  describe('migration should', () => {
    it('execute search, when finished', async () => {
      Store.get.mockImplementation(mockStoreGet());
      renderAndMigrate();
      await waitFor(() => expect(SearchActions.executeWithCurrentState).toHaveBeenCalled());
    });

    it('hide alert, when finished', async () => {
      Store.get.mockImplementation(mockStoreGet());
      const { queryByText } = renderAndMigrate();

      await waitFor(() => expect(queryByText('Migrate existing search page charts')).toBeNull());

      expect(Store.set).toHaveBeenCalledWith('pinned-field-charts-migrated', 'finished');
    });

    it('append new field chart widgets to existing widgets', async () => {
      const getNewWidgetPos = (actionMock) => {
        const widget = getNewWidget(actionMock);

        return actionMock.mock.calls[0][1].widgetPositions[widget.id];
      };

      const expWidgetPos = new WidgetPosition(1, 1, 4, Infinity);

      Store.get.mockImplementation(mockStoreGet());
      renderAndMigrate();
      const actionMock = asMock(ViewStatesActions.update);

      await waitFor(() => expect(actionMock.mock.calls[0][1].widgets.size).toEqual(3));

      expect(getNewWidgetPos(actionMock)).toEqual(expWidgetPos);
    });

    it('create row pivot with interval unit months, if field chart interval is quarter', async () => {
      const expPivotConfig = { interval: { type: 'timeunit', unit: 'months', value: 3 } };

      Store.get.mockImplementation(mockStoreGet({ interval: 'quarter' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).rowPivots[0].config).toEqual(expPivotConfig));
    });

    it('create row pivot with interval unit months, if field chart interval is month', async () => {
      const expPivotConfig = { interval: { type: 'timeunit', unit: 'months', value: 1 } };

      Store.get.mockImplementation(mockStoreGet({ interval: 'month' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).rowPivots[0].config).toEqual(expPivotConfig));

      expect(getNewWidgetConfig(ViewStatesActions.update).rowPivots[0].config).toEqual(expPivotConfig);
    });

    it('set visualization to scatter, if field chart visualization is scatter', async () => {
      Store.get.mockImplementation(mockStoreGet({ renderer: 'scatterplot' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualization).toEqual('scatter'));
    });

    it('set visualization to line, if field chart visualization is line', async () => {
      Store.get.mockImplementation(mockStoreGet({ renderer: 'line' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualization).toEqual('line'));
    });

    it('create visualization config with interpolation spline, if field chart interpolation is bundle', async () => {
      const expVisualizationConfg = new LineVisualizationConfig('spline');

      Store.get.mockImplementation(mockStoreGet({ interpolation: 'bundle' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create visualization config with interpolation linear, if field chart interpolation is linear', async () => {
      const expVisualizationConfg = new LineVisualizationConfig('linear');

      Store.get.mockImplementation(mockStoreGet({ interpolation: 'linear' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create area visualization config, if field chart visualization is area', async () => {
      const expVisualizationConfg = new AreaVisualizationConfig('linear');

      Store.get.mockImplementation(mockStoreGet({ renderer: 'area' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create line visualization config, if field chart visualization is line', async () => {
      const expVisualizationConfg = new LineVisualizationConfig('linear');

      Store.get.mockImplementation(mockStoreGet({ renderer: 'line' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create no visualization config, if field chart visualization is not line or area', async () => {
      Store.get.mockImplementation(mockStoreGet({ renderer: 'bar' }));
      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(undefined));

      expect(getNewWidgetConfig(ViewStatesActions.update).visualization).toEqual('bar');
    });

    it('create series config based on field chart series and field', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'count', field: 'messageCount' }));
      const newSeries = [new Series('count(messageCount)')];

      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });

    it('create series config with sum, if field chart series is total', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'total' }));
      const newSeries = [new Series('sum(level)')];

      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });

    it('create series config with avg, if field chart series is mean', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'mean' }));
      const newSeries = [new Series('avg(level)')];

      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });

    it('create series config with car, if field chart series is cardinality', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'cardinality' }));
      const newSeries = [new Series('card(level)')];

      renderAndMigrate();
      await waitFor(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });
  });
});
