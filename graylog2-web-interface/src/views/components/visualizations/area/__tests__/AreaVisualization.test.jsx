// @flow strict
import * as React from 'react';
import { mount } from 'enzyme';
import * as Immutable from 'immutable';

import mockComponent from 'helpers/mocking/MockComponent';
import { StoreMock as MockStore } from 'helpers/mocking';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import MockQuery from 'views/logic/queries/Query';

import AreaVisualization from '../AreaVisualization';
import { effectiveTimerange, simpleChartData } from './AreaVisualization.fixtures';

jest.mock('../../GenericPlot', () => mockComponent('GenericPlot'));
jest.mock('views/stores/CurrentQueryStore', () => ({
  CurrentQueryStore: MockStore(['getInitialState', () => MockQuery.builder().build()], 'listen'),
}));

describe('AreaVisualization', () => {
  it('generates correct props for plot component', () => {
    const config = AggregationWidgetConfig.builder()
      .visualization('area')
      .columnPivots([])
      .rowPivots([Pivot.create('timestamp', 'time', { interval: { type: 'timeunit', unit: 'minutes' } })])
      .series([Series.forFunction('avg(nf_bytes)'), Series.forFunction('sum(nf_pkts)')])
      .build();

    const wrapper = mount((<AreaVisualization config={config}
                                              data={simpleChartData}
                                              effectiveTimerange={effectiveTimerange}
                                              fields={Immutable.List()}
                                              height={1024}
                                              onChange={() => {}}
                                              toggleEdit={() => {}}
                                              width={800} />));

    const genericPlot = wrapper.find('GenericPlot');
    expect(genericPlot).toHaveProp('layout', {
      yaxis: { fixedrange: true, rangemode: 'tozero' },
      xaxis: { range: ['2019-11-28T15:21:00Z', '2019-11-28T15:25:57Z'], type: 'date' },
    });
    expect(genericPlot).toHaveProp('chartData', [
      {
        type: 'scatter',
        name: 'avg(nf_bytes)',
        x: [
          '2019-11-28T15:21:00.000+00:00',
          '2019-11-28T15:22:00.000+00:00',
          '2019-11-28T15:23:00.000+00:00',
          '2019-11-28T15:24:00.000+00:00',
          '2019-11-28T15:25:00.000+00:00'.
        ],
        y: [24558.239393939395, 3660.5666666666666, 49989.69, 2475.225, 10034.822222222223],
        fill: 'tozeroy',
        line: { shape: 'linear' },
      },
      {
        type: 'scatter',
        name: 'sum(nf_pkts)',
        x: [
          '2019-11-28T15:21:00.000+00:00',
          '2019-11-28T15:22:00.000+00:00',
          '2019-11-28T15:23:00.000+00:00',
          '2019-11-28T15:24:00.000+00:00',
          '2019-11-28T15:25:00.000+00:00'.
        ],
        y: [14967, 1239, 20776, 1285, 4377],
        fill: 'tozeroy',
        line: { shape: 'linear' },
      },
    ]);
  });
});
