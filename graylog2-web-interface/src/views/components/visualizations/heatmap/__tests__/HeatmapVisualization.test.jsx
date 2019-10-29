import React from 'react';
import { mount } from 'enzyme';

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
    const wrapper = mount(<HeatmapVisualization data={fixtures.validData} config={config} />);
    expect(wrapper).toMatchSnapshot();
  });
});
