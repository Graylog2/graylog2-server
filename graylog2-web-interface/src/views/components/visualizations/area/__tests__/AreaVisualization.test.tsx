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
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';
import mockComponent from 'helpers/mocking/MockComponent';
import { StoreMock as MockStore } from 'helpers/mocking';

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import MockQuery from 'views/logic/queries/Query';

import { effectiveTimerange, simpleChartData } from './AreaVisualization.fixtures';

import AreaVisualization from '../AreaVisualization';

jest.mock('../../GenericPlot', () => mockComponent('GenericPlot'));

jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: MockStore(['getInitialState', () => MockQuery.builder().build()], 'listen'),
}));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  rootTimeZone: jest.fn(() => 'America/Chicago'),
  gl2ServerUrl: jest.fn(() => undefined),
}));

describe('AreaVisualization', () => {
  it('generates correct props for plot component', () => {
    const config = AggregationWidgetConfig.builder()
      .visualization('area')
      .columnPivots([])
      .rowPivots([Pivot.create('timestamp', 'time', { interval: { type: 'timeunit', unit: 'minutes' } })])
      .series([Series.forFunction('avg(nf_bytes)'), Series.forFunction('sum(nf_pkts)')])
      .build();

    const wrapper = mount(<AreaVisualization config={config}
                                             data={simpleChartData}
                                             effectiveTimerange={effectiveTimerange}
                                             fields={Immutable.List()}
                                             height={1024}
                                             onChange={() => {}}
                                             toggleEdit={() => {}}
                                             width={800} />);

    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', expect.objectContaining({
      xaxis: { range: ['2019-11-28T09:21:00-06:00', '2019-11-28T09:25:57-06:00'], type: 'date' },
      legend: { y: -0.14 },
    }));

    expect(genericPlot).toHaveProp('chartData', [
      {
        type: 'scatter',
        name: 'avg(nf_bytes)',
        x: [
          '2019-11-28T09:21:00.000-06:00',
          '2019-11-28T09:22:00.000-06:00',
          '2019-11-28T09:23:00.000-06:00',
          '2019-11-28T09:24:00.000-06:00',
          '2019-11-28T09:25:00.000-06:00',
        ],
        y: [24558.239393939395, 3660.5666666666666, 49989.69, 2475.225, 10034.822222222223],
        fill: 'tozeroy',
        line: { shape: 'linear' },
      },
      {
        type: 'scatter',
        name: 'sum(nf_pkts)',
        x: [
          '2019-11-28T09:21:00.000-06:00',
          '2019-11-28T09:22:00.000-06:00',
          '2019-11-28T09:23:00.000-06:00',
          '2019-11-28T09:24:00.000-06:00',
          '2019-11-28T09:25:00.000-06:00',
        ],
        y: [14967, 1239, 20776, 1285, 4377],
        fill: 'tozeroy',
        line: { shape: 'linear' },
      },
    ]);
  });
});
