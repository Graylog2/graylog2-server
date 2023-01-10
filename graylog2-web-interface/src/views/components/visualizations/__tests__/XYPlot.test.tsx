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
import { mount } from 'wrappedEnzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import { alice as currentUser } from 'fixtures/users';
import asMock from 'helpers/mocking/AsMock';
import type { Props as XYPlotProps } from 'views/components/visualizations/XYPlot';
import XYPlot from 'views/components/visualizations/XYPlot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Query from 'views/logic/queries/Query';
import { QueriesActions } from 'views/stores/QueriesStore';
import { ALL_MESSAGES_TIMERANGE } from 'views/Constants';
import useCurrentQuery from 'views/logic/queries/useCurrentQuery';
import useViewType from 'views/hooks/useViewType';
import View from 'views/logic/views/View';
import useCurrentQueryId from 'views/logic/queries/useCurrentQueryId';

jest.mock('../GenericPlot', () => mockComponent('GenericPlot'));
jest.mock('views/stores/QueriesStore');
jest.mock('views/logic/queries/useCurrentQuery');
jest.mock('views/logic/queries/useCurrentQueryId');
jest.mock('views/hooks/useViewType');

describe('XYPlot', () => {
  const currentQuery = Query.fromJSON({ id: 'dummyquery', query: {}, timerange: {}, search_types: {} });
  const timestampPivot = new Pivot('timestamp', 'time', { interval: { type: 'auto', scaling: 1.0 } });
  const config = AggregationWidgetConfig.builder().rowPivots([timestampPivot]).build();
  const getChartColor = () => undefined;
  const setChartColor = () => ({});
  const chartData = [{ y: [23, 42], name: 'count()' }];

  const SimpleXYPlot = (props: Partial<XYPlotProps>) => (
    <XYPlot chartData={chartData}
            config={config}
            getChartColor={getChartColor}
            setChartColor={setChartColor}
            {...props} />
  );

  SimpleXYPlot.defaultProps = {
    currentUser,
    config: config,
    chartData: chartData,
    currentQuery: currentQuery,
    getChartColor: getChartColor,
    setChartColor: setChartColor,
  };

  beforeEach(() => {
    asMock(QueriesActions.timerange).mockReturnValueOnce(Promise.resolve(Immutable.OrderedMap()));
    asMock(useCurrentQuery).mockReturnValue(currentQuery);
    asMock(useCurrentQueryId).mockReturnValue(currentQuery.id);
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
      showlegend: false,
      hovermode: 'x',
    });

    expect(genericPlot).toHaveProp('chartData', chartData);

    genericPlot.get(0).props.onZoom('from', 'to');

    expect(QueriesActions.timerange).not.toHaveBeenCalled();
  });

  it('adds zoom handler for timeline plot', () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      xaxis: { range: ['2018-10-12T04:04:21.723+02:00', '2018-10-12T12:04:21.723+02:00'], type: 'date' },
    }));

    genericPlot.get(0).props.onZoom('2018-10-12T04:04:21.723Z', '2018-10-12T08:04:21.723Z');

    expect(QueriesActions.timerange).toHaveBeenCalledWith('dummyquery', {
      type: 'absolute',
      from: '2018-10-12T04:04:21.723+00:00',
      to: '2018-10-12T08:04:21.723+00:00',
    });
  });

  it('uses effective time range from pivot result if all messages are selected', () => {
    const timerange = { from: '2018-10-12T02:04:21.723Z', to: '2018-10-12T10:04:21.723Z', type: 'absolute' };
    const currentQueryForAllMessages = currentQuery.toBuilder().timerange(ALL_MESSAGES_TIMERANGE).build();
    const user = currentUser.toBuilder().timezone('UTC').build();
    const wrapper = mount(<SimpleXYPlot effectiveTimerange={timerange}
                                        currentQuery={currentQueryForAllMessages}
                                        currentUser={user} />);
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
