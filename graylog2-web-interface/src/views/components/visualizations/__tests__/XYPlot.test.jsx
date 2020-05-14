// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import mockComponent from 'helpers/mocking/MockComponent';

import asMock from 'helpers/mocking/AsMock';
import XYPlot from 'views/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Query from 'views/logic/queries/Query';
import { QueriesActions } from 'views/stores/QueriesStore';
import { SearchActions } from 'views/stores/SearchStore';
import CurrentUserStore from 'stores/users/CurrentUserStore';

jest.mock('stores/users/CurrentUserStore', () => ({
  get: jest.fn(),
}));

jest.mock('views/stores/SearchStore', () => ({
  SearchStore: {},
  // eslint-disable-next-line global-require
  SearchActions: {
    executeWithCurrentState: jest.fn(),
  },
}));
jest.mock('stores/connect', () => (x) => x);
jest.mock('../GenericPlot', () => mockComponent('GenericPlot'));
jest.mock('views/stores/QueriesStore');

describe('XYPlot', () => {
  const currentQuery = Query.fromJSON({ id: 'dummyquery', query: {}, timerange: {}, search_types: {} });
  const timestampPivot = new Pivot('timestamp', 'time', {});
  const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
  const getCurrentChartColor = () => {};
  const getPinnedChartColor = () => {};
  const chartData = [{ y: [23, 42] }];

  beforeEach(() => {
    asMock(QueriesActions.timerange).mockReturnValueOnce(Promise.resolve());
  });

  it('renders generic X/Y-Plot when no timeline config is passed', () => {
    const emptyConfig = AggregationWidgetConfig.builder().build();
    const timerange = { from: 'foo', to: 'bar' };
    const wrapper = mount((
      <XYPlot chartData={chartData}
              config={emptyConfig}
              getCurrentChartColor={getCurrentChartColor}
              getPinnedChartColor={getPinnedChartColor}
              timezone="UTC"
              currentQuery={currentQuery}
              effectiveTimerange={timerange} />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', { yaxis: { fixedrange: true, rangemode: 'tozero' }, xaxis: { fixedrange: true } });
    expect(genericPlot).toHaveProp('chartData', chartData);

    genericPlot.get(0).props.onZoom('from', 'to');
    expect(QueriesActions.timerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', () => {
    CurrentUserStore.get.mockReturnValue({ timezone: 'UTC' });
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z' };
    const wrapper = mount((
      <XYPlot chartData={chartData}
              getCurrentChartColor={getCurrentChartColor}
              getPinnedChartColor={getPinnedChartColor}
              config={config}
              timezone="UTC"
              currentQuery={currentQuery}
              effectiveTimerange={timerange} />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero' },
      xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'], type: 'date' },
    });

    genericPlot.get(0).props.onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');

    expect(SearchActions.executeWithCurrentState).not.toHaveBeenCalled();
    expect(QueriesActions.timerange).toHaveBeenCalledWith('dummyquery', {
      type: 'absolute',
      from: '2018-10-12T04:04:21.723Z',
      to: '2018-10-12T08:04:21.723Z',
    });
  });

  it('uses effective time range from pivot result if all messages are selected', () => {
    const effectiveTimerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z' };
    const allMessages = { type: 'relative', range: 0 };
    const currentQueryForAllMessages = currentQuery.toBuilder().timerange(allMessages).build();

    const wrapper = mount((
      <XYPlot chartData={chartData}
              getCurrentChartColor={getCurrentChartColor}
              getPinnedChartColor={getPinnedChartColor}
              config={config}
              timezone="UTC"
              currentQuery={currentQueryForAllMessages}
              effectiveTimerange={effectiveTimerange} />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero' },
      xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'], type: 'date' },
    });
  });

  it('sets correct plot legend position for small containers', () => {
    const wrapper = mount((
      <XYPlot chartData={chartData}
              getCurrentChartColor={getCurrentChartColor}
              getPinnedChartColor={getPinnedChartColor}
              height={140}
              config={config}
              currentQuery={currentQuery}
              timezone="UTC" />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero' },
      xaxis: { fixedrange: true },
      legend: { y: -0.6 },
    });
  });

  it('sets correct plot legend position for containers with medium height', () => {
    const wrapper = mount((
      <XYPlot chartData={chartData}
              getCurrentChartColor={getCurrentChartColor}
              getPinnedChartColor={getPinnedChartColor}
              height={350}
              config={config}
              currentQuery={currentQuery}
              timezone="UTC" />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero' },
      xaxis: { fixedrange: true },
      legend: { y: -0.2 },
    });
  });

  it('sets correct plot legend position for containers with huge height', () => {
    const wrapper = mount((
      <XYPlot chartData={chartData}
              getCurrentChartColor={getCurrentChartColor}
              getPinnedChartColor={getPinnedChartColor}
              height={700}
              config={config}
              currentQuery={currentQuery}
              timezone="UTC" />
    ));
    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero' },
      xaxis: { fixedrange: true },
      legend: { y: -0.14 },
    });
  });
});
