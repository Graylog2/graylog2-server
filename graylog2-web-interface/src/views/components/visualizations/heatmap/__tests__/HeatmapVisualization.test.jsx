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
import React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';
import mockComponent from 'helpers/mocking/MockComponent';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import * as fixtures from './HeatmapVisualization.fixtures';

import HeatmapVisualization from '../HeatmapVisualization';

jest.mock('../../GenericPlot', () => mockComponent('GenericPlot'));

describe('HeatmapVisualization', () => {
  it('generates correct props for plot component', () => {
    const columnPivot = new Pivot('http_status', 'values');
    const rowPivot = new Pivot('hour', 'values');
    const series = new Series('count()');
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot]).series([series])
      .visualization('heatmap')
      .build();
    const effectiveTimerange = { type: 'absolute', from: '2019-10-22T11:54:35.850Z', to: '2019-10-29T11:53:50.000Z' };
    const plotLayout = { yaxis: { type: 'category', fixedrange: true }, xaxis: { type: 'category', fixedrange: true }, plot_bgcolor: '#440154', margin: { b: 40 } };
    const plotChartData = [
      {
        type: 'heatmap',
        name: 'Heatmap Chart',
        x: ['100', '201', '304', '405'],
        y: ['00', '01'],
        z: [[217, 'None', 213, 'None'], ['None', 217, 'None', 230]],
        text: [['count()', 'count()', 'count()', 'count()'], ['count()', 'count()', 'count()', 'count()']],
        customdata: [[217, 'None', 213, 'None'], ['None', 217, 'None', 230]],
        hovertemplate: 'hour: %{y}<br>http_status: %{x}<br>%{text}: %{customdata}<extra></extra>',
        colorscale: [[0, '#440154'], [0.05, '#481567'], [0.1, '#483677'], [0.15, '#453781'], [0.2, '#404788'], [0.3, '#39568c'], [0.35, '#33638d'], [0.4, '#2d708e'], [0.45, '#287d8e'], [0.5, '#238a8d'], [0.55, '#1f968b'], [0.6, '#20a387'], [0.65, '#29af7f'], [0.7, '#3cbb75'], [0.75, '#55c667'], [0.8, '#73d055'], [0.85, '#95d840'], [0.9, '#b8de29'], [0.95, '#dce319'], [1, '#fde725']],
      },
    ];

    const wrapper = mount(<HeatmapVisualization data={fixtures.validData}
                                                config={config}
                                                effectiveTimerange={effectiveTimerange}
                                                fields={Immutable.List()}
                                                height={1024}
                                                onChange={() => {}}
                                                toggleEdit={() => {}}
                                                width={800} />);
    const genericPlot = wrapper.find('GenericPlot');

    expect(genericPlot).toHaveProp('layout', plotLayout);
    expect(genericPlot).toHaveProp('chartData', plotChartData);
  });
});
