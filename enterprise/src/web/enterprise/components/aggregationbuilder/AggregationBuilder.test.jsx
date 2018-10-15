import React from 'react';
import { mount } from 'enzyme';

import AggregationWidgetConfig from 'enterprise/logic/aggregationbuilder/AggregationWidgetConfig';
import AggregationBuilder from './AggregationBuilder';
import EmptyResultWidget from '../widgets/EmptyResultWidget';

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
  it('renders helpful advice instead of visualization when no documents were in result', () => {
    const wrapper = mount(<AggregationBuilder data={{ total: 0 }}
                                              config={AggregationWidgetConfig.builder().build()}
                                              fields={{}}
                                              onChange={() => {}} />);

    expect(wrapper.find(EmptyResultWidget)).toHaveLength(1);
  });

  it('renders dummy component with rows from data', () => {
    const wrapper = mount(<AggregationBuilder config={AggregationWidgetConfig.builder().visualization('dummy').build()}
                                              fields={{}}
                                              onChange={() => {}}
                                              data={{ total: 42, rows: [{ value: 3.1415926 }] }} />);

    expect(wrapper.find(EmptyResultWidget)).toHaveLength(0);
    const dummyVisualization = wrapper.find(mockDummyVisualization);
    expect(dummyVisualization).toHaveLength(1);
    expect(dummyVisualization).toHaveProp('data', [{ value: 3.1415926 }]);
  });
});
