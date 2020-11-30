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
// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { mount } from 'wrappedEnzyme';
import mockComponent from 'helpers/mocking/MockComponent';
import { viewsManager } from 'fixtures/users';
import asMock from 'helpers/mocking/AsMock';
import { $PropertyType } from 'utility-types';

import type { UserJSON } from 'logic/users/User';
import CurrentUserContext from 'contexts/CurrentUserContext';
import XYPlot, { Props as XYPlotProps } from 'views/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Query, { RelativeTimeRange } from 'views/logic/queries/Query';
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
  const getChartColor = () => {};
  const setChartColor = () => ({});
  const chartData = [{ y: [23, 42] }];
  type SimpleXYPlotProps = {
    currentUser?: UserJSON,
    config?: $PropertyType<XYPlotProps, 'chartData'>,
    chartData?: $PropertyType<XYPlotProps, 'chartData'>,
    currentQuery?: $PropertyType<XYPlotProps, 'currentQuery'>,
    effectiveTimerange?: $PropertyType<XYPlotProps, 'effectiveTimerange'>,
    getChartColor?: $PropertyType<XYPlotProps, 'getChartColor'>,
    height?: $PropertyType<XYPlotProps, 'height'>,
    setChartColor?: $PropertyType<XYPlotProps, 'setChartColor'>,
    plotLayout?: $PropertyType<XYPlotProps, 'plotLayout'>,
    onZoom?: $PropertyType<XYPlotProps, 'onZoom'>,
  };

  const SimpleXYPlot = ({ currentUser, ...props }: SimpleXYPlotProps) => (
    <CurrentUserContext.Provider value={currentUser}>
      <XYPlot chartData={chartData}
              config={config}
              getChartColor={getChartColor}
              setChartColor={setChartColor}
              currentQuery={currentQuery}
              {...props} />
    </CurrentUserContext.Provider>
  );

  SimpleXYPlot.defaultProps = {
    currentUser: viewsManager,
    config: config,
    chartData: chartData,
    currentQuery: currentQuery,
    effectiveTimerange: undefined,
    getChartColor: getChartColor,
    height: undefined,
    setChartColor: setChartColor,
    plotLayout: undefined,
    onZoom: undefined,
  };

  beforeEach(() => {
    asMock(QueriesActions.timerange).mockReturnValueOnce(Promise.resolve(Immutable.OrderedMap()));
  });

  it('renders generic X/Y-Plot when no timeline config is passed', () => {
    const emptyConfig = AggregationWidgetConfig.builder().build();
    const timerange = { from: 'foo', to: 'bar', type: 'absolute' };
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange} config={emptyConfig} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero', tickformat: ',g' },
      xaxis: { fixedrange: true },
    });

    expect(genericPlot).toHaveProp('chartData', chartData);

    genericPlot.get(0).props.onZoom('from', 'to');

    expect(QueriesActions.timerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', () => {
    CurrentUserStore.get.mockReturnValue({ timezone: 'UTC' });
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange} currentUser={{ ...viewsManager, timezone: 'UTC' }} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'], type: 'date' },
    }));

    genericPlot.get(0).props.onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');

    expect(SearchActions.executeWithCurrentState).not.toHaveBeenCalled();

    expect(QueriesActions.timerange).toHaveBeenCalledWith('dummyquery', {
      type: 'absolute',
      from: '2018-10-12T04:04:21.723Z',
      to: '2018-10-12T08:04:21.723Z',
    });
  });

  it('uses effective time range from pivot result if all messages are selected', () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const allMessages: RelativeTimeRange = { type: 'relative', range: 0 };
    const currentQueryForAllMessages = currentQuery.toBuilder().timerange(allMessages).build();
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange} currentQuery={currentQueryForAllMessages} currentUser={{ ...viewsManager, timezone: 'UTC' }} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      xaxis: { range: ['2018-10-12T02:04:21Z', '2018-10-12T10:04:21Z'], type: 'date' },
    }));
  });

  it('sets correct plot legend position for small containers', () => {
    const wrapper = mount(<SimpleXYPlot height={140} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      legend: { y: -0.6 },
    }));
  });

  it('sets correct plot legend position for containers with medium height', () => {
    const wrapper = mount(<SimpleXYPlot height={350} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      legend: { y: -0.2 },
    }));
  });

  it('sets correct plot legend position for containers with huge height', () => {
    const wrapper = mount(<SimpleXYPlot height={700} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      legend: { y: -0.14 },
    }));
  });
});
