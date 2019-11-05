// @flow strict
import React from 'react';
import { mount } from 'enzyme';
import * as Immutable from 'immutable';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import HeatmapVisualization from '../HeatmapVisualization';
import * as fixtures from './HeatmapVisualization.fixtures';

describe('HeatmapVisualization', () => {
  it('renders correctly', () => {
    const columnPivot = new Pivot('http_status', 'values');
    const rowPivot = new Pivot('hour', 'values');
    const series = new Series('count()');
    const config = AggregationWidgetConfig.builder()
      .rowPivots([rowPivot])
      .columnPivots([columnPivot]).series([series])
      .visualization('heatmap')
      .build();
    const effectiveTimerange = { type: 'absolute', from: '2019-10-22T11:54:35.850Z', to: '2019-10-29T11:53:50.000Z' };
    const wrapper = mount(<HeatmapVisualization data={fixtures.validData}
                                                config={config}
                                                effectiveTimerange={effectiveTimerange}
                                                fields={Immutable.List()}
                                                height={1024}
                                                onChange={() => {}}
                                                toggleEdit={() => {}}
                                                width={800} />);
    expect(wrapper).toMatchSnapshot();
  });
});
