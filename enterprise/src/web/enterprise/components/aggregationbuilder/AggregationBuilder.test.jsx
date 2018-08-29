import React from 'react';
import { mount } from 'enzyme';

import mockComponent from 'helpers/mocking/MockComponent';
import AggregationBuilder from './AggregationBuilder';
import EmptyResultWidget from '../widgets/EmptyResultWidget';

const mockDummyVisualization = mockComponent('DummyVisualization');
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
    const wrapper = mount(<AggregationBuilder data={{ total: 0 }} />);

    expect(wrapper.find(EmptyResultWidget)).toHaveLength(1);
  });

  it('renders dummy component with rows from data', () => {
    const wrapper = mount(<AggregationBuilder config={{ visualization: 'dummy' }}data={{ total: 42, rows: [{ value: 3.1415926 }] }} />);

    expect(wrapper.find(EmptyResultWidget)).toHaveLength(0);
    const dummyVisualization = wrapper.find(mockDummyVisualization);
    expect(dummyVisualization).toHaveLength(1);
    expect(dummyVisualization).toHaveProp('data', [{ value: 3.1415926 }]);
  });
});
