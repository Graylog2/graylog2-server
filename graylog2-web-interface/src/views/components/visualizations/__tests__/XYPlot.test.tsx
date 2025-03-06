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
import { render, waitFor } from 'wrappedTestingLibrary';

import mockComponent from 'helpers/mocking/MockComponent';
import asMock from 'helpers/mocking/AsMock';
import type { Props as XYPlotProps } from 'views/components/visualizations/XYPlot';
import XYPlot from 'views/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Query, { createElasticsearchQueryString } from 'views/logic/queries/Query';
import { ALL_MESSAGES_TIMERANGE, DEFAULT_TIMERANGE } from 'views/Constants';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import { createSearch } from 'fixtures/searches';
import { setTimerange } from 'views/logic/slices/viewSlice';
import GenericPlot from 'views/components/visualizations/GenericPlot';

jest.mock('../GenericPlot', () => jest.fn(mockComponent('GenericPlot')));
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/logic/queries/useCurrentQueryId');
jest.mock('views/hooks/useViewType');

jest.mock('views/logic/slices/viewSlice', () => ({
  ...jest.requireActual('views/logic/slices/viewSlice'),
  setTimerange: jest.fn(() => async () => {}),
}));

const defaultCurrentQuery = Query.fromJSON({
  id: 'dummyquery',
  query: createElasticsearchQueryString(),
  timerange: DEFAULT_TIMERANGE,
  search_types: [],
});

describe('XYPlot', () => {
  const timestampPivot = Pivot.create(['timestamp'], 'time', { interval: { type: 'auto', scaling: 1.0 } });
  const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
  const setChartColor = () => ({});
  const chartData = [{ y: [23, 42], name: 'count()' }];

  const SimpleXYPlot = ({
    currentQuery = defaultCurrentQuery,
    ...props
  }: Partial<XYPlotProps> & { currentQuery?: Query }) => {
    const defaultView = createSearch();
    const view = defaultView
      .toBuilder()
      .type(View.Type.Search)
      .search(defaultView.search.toBuilder().queries([currentQuery]).build())
      .build();

    return (
      <TestStoreProvider view={view} initialQuery={currentQuery.id}>
        <XYPlot
          chartData={chartData}
          config={config}
          setChartColor={setChartColor}
          height={480}
          width={640}
          {...props}
        />
      </TestStoreProvider>
    );
  };

  useViewsPlugin();

  beforeEach(() => {
    asMock(useCurrentQuery).mockReturnValue(defaultCurrentQuery);
    asMock(useCurrentQueryId).mockReturnValue(defaultCurrentQuery.id);
    asMock(useViewType).mockReturnValue(View.Type.Search);
    jest.clearAllMocks();
  });

  it('renders generic X/Y-Plot when no timeline config is passed', () => {
    const emptyConfig = AggregationWidgetConfig.builder().build();
    const timerange = { from: 'foo', to: 'bar', type: 'absolute' };
    render(<SimpleXYPlot effectiveTimerange={timerange} config={emptyConfig} />);

    expect(GenericPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        layout: {
          yaxis: { fixedrange: true, rangemode: 'tozero', tickformat: ',~r', type: 'linear' },
          xaxis: { fixedrange: true },
          hovermode: 'x',
          legend: { y: -0.14 },
        },
        chartData: expect.anything(),
      }),
      expect.anything(),
    );

    const { onZoom } = asMock(GenericPlot).mock.calls[0][0];
    onZoom('from', 'to');

    expect(setTimerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', async () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    render(<SimpleXYPlot effectiveTimerange={timerange} />);

    expect(GenericPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        'layout': expect.objectContaining({
          xaxis: { range: ['2018-10-12T04:04:21.723+02:00', '2018-10-12T12:04:21.723+02:00'], type: 'date' },
        }),
      }),
      {},
    );

    const { onZoom } = asMock(GenericPlot).mock.calls[0][0];
    onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');

    await waitFor(() => {
      expect(setTimerange).toHaveBeenCalledWith('dummyquery', {
        type: 'absolute',
        from: '2018-10-12T04:04:21.723+00:00',
        to: '2018-10-12T08:04:21.723+00:00',
      });
    });
  });

  it('uses effective time range from pivot result if all messages are selected', () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const currentQueryForAllMessages = defaultCurrentQuery.toBuilder().timerange(ALL_MESSAGES_TIMERANGE).build();
    render(<SimpleXYPlot effectiveTimerange={timerange} currentQuery={currentQueryForAllMessages} />);

    expect(GenericPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        'layout': expect.objectContaining({
          xaxis: { range: ['2018-10-12T04:04:21.723+02:00', '2018-10-12T12:04:21.723+02:00'], type: 'date' },
        }),
      }),
      {},
    );
  });

  it('sets correct plot legend position for small containers', () => {
    render(<SimpleXYPlot height={140} />);

    expect(GenericPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        'layout': expect.objectContaining({
          legend: { y: -0.6 },
        }),
      }),
      {},
    );
  });

  it('sets correct plot legend position for containers with medium height', () => {
    render(<SimpleXYPlot height={350} />);

    expect(GenericPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        'layout': expect.objectContaining({
          legend: { y: -0.2 },
        }),
      }),
      {},
    );
  });

  it('sets correct plot legend position for containers with huge height', () => {
    render(<SimpleXYPlot height={700} />);

    expect(GenericPlot).toHaveBeenCalledWith(
      expect.objectContaining({
        'layout': expect.objectContaining({
          legend: { y: -0.14 },
        }),
      }),
      {},
    );
  });
});
