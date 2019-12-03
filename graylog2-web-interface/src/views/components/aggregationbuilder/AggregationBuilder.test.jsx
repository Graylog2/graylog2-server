import React from 'react';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';

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
  it('falls back to retrieving effective timerange from first result if no `chart` result present', () => {
    const data = {
      '524d182c-8e32-4372-b30d-a40d99efe55d': {
        total: 42,
        rows: [{ value: 3.1415926 }],
        effective_timerange: 42,
      },
    };
    const wrapper = mount(<AggregationBuilder config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
                                              fields={{}}
                                              data={data} />);
    const dummyVisualization = wrapper.find(mockDummyVisualization);
    expect(dummyVisualization).toHaveProp('effectiveTimerange', 42);
  });
});
