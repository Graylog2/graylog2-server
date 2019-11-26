import React from 'react';
import { mount } from 'enzyme';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationBuilder from './AggregationBuilder';
import EmptyResultWidget from '../widgets/EmptyResultWidget';
import EmptyAggregationContent from './EmptyAggregationContent';

const mockDummyVisualization = () => 'dummy-visualization';
jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => ([
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        component: mockDummyVisualization,
      },
    ]),
  },
}));

describe('AggregationBuilder', () => {
  const rowPivot = Pivot.create('field', 'string');

  it('does render empty result widget when no documents were in result and is edit', () => {
    const wrapper = mount(<AggregationBuilder data={{ total: 0 }}
                                              editing
                                              config={AggregationWidgetConfig.builder().visualization('dummy').build()}
                                              fields={{}} />);

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(1);
    expect(wrapper.find(EmptyAggregationContent)).toHaveProp('editing', true);
  });

  it('renders dummy component with rows from data', () => {
    const wrapper = mount(<AggregationBuilder config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
                                              fields={{}}
                                              data={{ chart: { total: 42, rows: [{ value: 3.1415926 }] } }} />);

    expect(wrapper.find(EmptyResultWidget)).toHaveLength(0);
    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(0);
    const dummyVisualization = wrapper.find(mockDummyVisualization);
    expect(dummyVisualization).toHaveLength(1);
    expect(dummyVisualization).toHaveProp('data', { chart: [{ value: 3.1415926 }] });
  });
  it('passes through onVisualizationConfigChange to visualization', () => {
    const onVisualizationConfigChange = jest.fn();
    const wrapper = mount(<AggregationBuilder config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
                                              fields={{}}
                                              onVisualizationConfigChange={onVisualizationConfigChange}
                                              data={{ total: 42, rows: [{ value: 3.1415926 }] }} />);

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(0);
    const dummyVisualization = wrapper.find(mockDummyVisualization);
    expect(dummyVisualization).toHaveProp('onChange', onVisualizationConfigChange);
  });
  it('renders EmptyAggregationContent if the AggregationWidgetConfig is empty', () => {
    const wrapper = mount(<AggregationBuilder config={AggregationWidgetConfig.builder().visualization('dummy').build()}
                                              fields={{}}
                                              data={{ total: 42, rows: [{ value: 3.1415926 }] }} />);

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(1);
    expect(wrapper.find(EmptyAggregationContent)).toHaveProp('editing', false);
  });
});
