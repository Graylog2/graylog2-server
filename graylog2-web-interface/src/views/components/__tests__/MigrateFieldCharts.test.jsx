// @flow strict
import React from 'react';
import { render, fireEvent, wait, waitForElementToBeRemoved, cleanup } from '@testing-library/react';
import { StoreMock as MockStore } from 'helpers/mocking';

import LineVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/LineVisualizationConfig';
import AreaVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/AreaVisualizationConfig';
import Series from 'views/logic/aggregationbuilder/Series';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import Store from 'logic/local-storage/Store';
import SearchActions from 'views/actions/SearchActions';
import { ViewStatesActions } from 'views/stores/ViewStatesStore';

import MigrateFieldCharts from '../MigrateFieldCharts';
import { mockFieldCharts, viewState as mockViewState } from './MigrateFieldCharts.fixtures';

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
  const viewState = actionMock.mock.calls[0][1];
  const widgetsTotal = viewState.widgets.size;
  // console.log('the widget', viewState.widgets.get(widgetsTotal - 1).config);
  return viewState.widgets.get(widgetsTotal - 1);
};

const getNewWidgetConfig = (actionMock) => {
  return getNewWidget(actionMock).config;
};

const renderAndMigrate = () => {
  const { queryByText } = render(<MigrateFieldCharts />);
  const migrateButton = queryByText('Migrate');
  fireEvent.click(migrateButton);
  return { queryByText };
};

describe('MigrateFieldCharts', () => {
  afterEach(() => {
    cleanup();
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
      await wait(() => expect(SearchActions.executeWithCurrentState).toHaveBeenCalled());
    });

    it('hide alert, when finished', async () => {
      Store.get.mockImplementation(mockStoreGet());
      const { queryByText } = renderAndMigrate();
      await waitForElementToBeRemoved(() => queryByText('Migrate existing search page charts'));
      expect(Store.set).toHaveBeenCalledWith('pinned-field-charts-migrated', true);
    });

    it('append new field chart widgets to existing widgets', async () => {
      const getNewWidgetPos = (actionMock) => {
        const widget = getNewWidget(actionMock);
        return actionMock.mock.calls[0][1].widgetPositions[widget.id];
      };
      const expWidgetPos = new WidgetPosition(1, 9, 4, Infinity);
      Store.get.mockImplementation(mockStoreGet());
      renderAndMigrate();
      await wait(() => expect(ViewStatesActions.update.mock.calls[0][1].widgets.size).toEqual(3));
      expect(getNewWidgetPos(ViewStatesActions.update)).toEqual(expWidgetPos);
    });

    it('create row pivot with interval unit months, if field chart interval is quarter', async () => {
      const expPivotConfig = { interval: { type: 'timeunit', unit: 'months', value: 3 } };
      Store.get.mockImplementation(mockStoreGet({ interval: 'quarter' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).rowPivots[0].config).toEqual(expPivotConfig));
    });

    it('create row pivot with interval unit months, if field chart interval is month', async () => {
      const expPivotConfig = { interval: { type: 'timeunit', unit: 'months', value: 1 } };
      Store.get.mockImplementation(mockStoreGet({ interval: 'month' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).rowPivots[0].config).toEqual(expPivotConfig));
      expect(getNewWidgetConfig(ViewStatesActions.update).rowPivots[0].config).toEqual(expPivotConfig);
    });

    it('set visualization to scatter, if field chart visualization is scatter', async () => {
      Store.get.mockImplementation(mockStoreGet({ renderer: 'scatterplot' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualization).toEqual('scatter'));
    });

    it('set visualization to line, if field chart visualization is line', async () => {
      Store.get.mockImplementation(mockStoreGet({ renderer: 'line' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualization).toEqual('line'));
    });

    it('create visualization config with interpolation spline, if field chart interpolation is bundle', async () => {
      const expVisualizationConfg = new LineVisualizationConfig('spline');
      Store.get.mockImplementation(mockStoreGet({ interpolation: 'bundle' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create visualization config with interpolation linear, if field chart interpolation is linear', async () => {
      const expVisualizationConfg = new LineVisualizationConfig('linear');
      Store.get.mockImplementation(mockStoreGet({ interpolation: 'linear' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create area visualization config, if field chart visualization is area', async () => {
      const expVisualizationConfg = new AreaVisualizationConfig('linear');
      Store.get.mockImplementation(mockStoreGet({ renderer: 'area' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create line visualization config, if field chart visualization is line', async () => {
      const expVisualizationConfg = new LineVisualizationConfig('linear');
      Store.get.mockImplementation(mockStoreGet({ renderer: 'line' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(expVisualizationConfg));
    });

    it('create no visualization config, if field chart visualization is not line or area', async () => {
      Store.get.mockImplementation(mockStoreGet({ renderer: 'bar' }));
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).visualizationConfig).toEqual(undefined));
      expect(getNewWidgetConfig(ViewStatesActions.update).visualization).toEqual('bar');
    });

    it('create series config based on field chart series and field', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'count', field: 'messageCount' }));
      const newSeries = [new Series('count(messageCount)')];
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });

    it('create series config with sum, if field chart series is total', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'total' }));
      const newSeries = [new Series('sum(level)')];
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });

    it('create series config with avg, if field chart series is mean', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'mean' }));
      const newSeries = [new Series('avg(level)')];
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });

    it('create series config with car, if field chart series is cardinality', async () => {
      Store.get.mockImplementation(mockStoreGet({ valuetype: 'cardinality' }));
      const newSeries = [new Series('card(level)')];
      renderAndMigrate();
      await wait(() => expect(getNewWidgetConfig(ViewStatesActions.update).series).toEqual(newSeries));
    });
  });
});
