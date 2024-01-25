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
import { mount } from 'wrappedEnzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import asMock from 'helpers/mocking/AsMock';
import type { Props as XYPlotProps } from 'views/components/visualizations/XYPlot';
import XYPlot from 'views/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Query from 'views/logic/queries/Query';
import { ALL_MESSAGES_TIMERANGE } from 'views/Constants';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import { setTimerange } from 'views/logic/slices/viewSlice';

jest.mock('../GenericPlot', () => mockComponent('GenericPlot'));
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/logic/queries/useCurrentQueryId');
jest.mock('views/hooks/useViewType');

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  setTimerange: jest.fn(() => async () => {}),
}));

const defaultCurrentQuery = Query.fromJSON({ id: 'dummyquery', query: {}, timerange: {}, search_types: {} });

describe('XYPlot', () => {
  const timestampPivot = Pivot.create(['timestamp'], 'time', { interval: { type: 'auto', scaling: 1.0 } });
  const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
  const setChartColor = () => ({});
  const chartData = [{ y: [23, 42], name: 'count()' }];

  const SimpleXYPlot = ({ currentQuery, ...props }: Partial<XYPlotProps> & { currentQuery?: Query }) => {
    const defaultView = createSearch();
    const view = defaultView
      .toBuilder()
      .type(View.Type.Search)
      .search(defaultView.search
        .toBuilder()
        .queries([currentQuery])
        .build())
      .build();

    return (
      <TestStoreProvider view={view} initialQuery={currentQuery.id}>
        <XYPlot chartData={chartData}
                config={config}
                setChartColor={setChartColor}
                {...props} />
      </TestStoreProvider>
    );
  };

  SimpleXYPlot.defaultProps = {
    currentQuery: defaultCurrentQuery,
  };

  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    asMock(useCurrentQuery).mockReturnValue(defaultCurrentQuery);
    asMock(useCurrentQueryId).mockReturnValue(defaultCurrentQuery.id);
    asMock(useViewType).mockReturnValue(View.Type.Search);
  });

  it('renders generic X/Y-Plot when no timeline config is passed', () => {
    const emptyConfig = AggregationWidgetConfig.builder().build();
    const timerange = { from: 'foo', to: 'bar', type: 'absolute' };
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange} config={emptyConfig} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero', tickformat: ',~r', type: 'linear' },
      xaxis: { fixedrange: true },
      hovermode: 'x',
    });

    expect(genericPlot).toHaveProp('chartData', chartData);

    genericPlot.get(0).props.onZoom('from', 'to');

    expect(setTimerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      xaxis: { range: ['2018-10-12T04:04:21.723+02:00', '2018-10-12T12:04:21.723+02:00'], type: 'date' },
    }));

    genericPlot.get(0).props.onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');

    expect(setTimerange).toHaveBeenCalledWith('dummyquery', {
      type: 'absolute',
      from: '2018-10-12T04:04:21.723+00:00',
      to: '2018-10-12T08:04:21.723+00:00',
    });
  });

  it('uses effective time range from pivot result if all messages are selected', () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const currentQueryForAllMessages = defaultCurrentQuery.toBuilder().timerange(ALL_MESSAGES_TIMERANGE).build();
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange}
                                        currentQuery={currentQueryForAllMessages} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      xaxis: { range: ['2018-10-12T04:04:21.723+02:00', '2018-10-12T12:04:21.723+02:00'], type: 'date' },
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
