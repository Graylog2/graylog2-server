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
import * as Immutable from 'immutable';

import mockComponent from 'helpers/mocking/MockComponent';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

import { effectiveTimerange, simpleChartData } from './AreaVisualization.fixtures';

import OriginalAreaVisualization from '../AreaVisualization';

jest.mock('../../GenericPlot', () => mockComponent('GenericPlot'));

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  rootTimeZone: jest.fn(() => 'America/Chicago'),
  gl2ServerUrl: jest.fn(() => undefined),
  isCloud: jest.fn(() => false),
}));

const AreaVisualization = (props: React.ComponentProps<typeof OriginalAreaVisualization>) => (
  <TestStoreProvider>
    <FieldTypesContext.Provider value={{ all: Immutable.List(), queryFields: Immutable.Map({ 'query-id-1': Immutable.List<FieldTypeMapping>() }) }}>
      <OriginalAreaVisualization {...props} />
    </FieldTypesContext.Provider>
  </TestStoreProvider>
);

describe('AreaVisualization', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  it('generates correct props for plot component', () => {
    const config = AggregationWidgetConfig.builder()
      .visualization('area')
      .columnPivots([])
      .rowPivots([Pivot.create(['timestamp'], 'time', { interval: { type: 'timeunit', unit: 'minutes', value: 10 } })])
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
      xaxis: { range: ['2019-11-28T16:21:00.486+01:00', '2019-11-28T16:25:57.000+01:00'], type: 'date' },
      legend: { y: -0.14 },
    }));

    expect(genericPlot).toHaveProp('chartData', [
      {
        type: 'scatter',
        name: 'avg(nf_bytes)',
        x: [
          '2019-11-28T16:21:00.000+01:00',
          '2019-11-28T16:22:00.000+01:00',
          '2019-11-28T16:23:00.000+01:00',
          '2019-11-28T16:24:00.000+01:00',
          '2019-11-28T16:25:00.000+01:00',
        ],
        y: [24558.239393939395, 3660.5666666666666, 49989.69, 2475.225, 10034.822222222223],
        fill: 'tozeroy',
        line: { shape: 'linear' },
        originalName: 'avg(nf_bytes)',
      },
      {
        type: 'scatter',
        name: 'sum(nf_pkts)',
        x: [
          '2019-11-28T16:21:00.000+01:00',
          '2019-11-28T16:22:00.000+01:00',
          '2019-11-28T16:23:00.000+01:00',
          '2019-11-28T16:24:00.000+01:00',
          '2019-11-28T16:25:00.000+01:00',
        ],
        y: [14967, 1239, 20776, 1285, 4377],
        fill: 'tozeroy',
        line: { shape: 'linear' },
        originalName: 'sum(nf_pkts)',
      },
    ]);
  });
});
