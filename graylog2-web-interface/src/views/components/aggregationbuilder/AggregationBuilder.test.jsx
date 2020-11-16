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
import React from 'react';
import { mount } from 'wrappedEnzyme';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import AggregationBuilder from './AggregationBuilder';
import EmptyAggregationContent from './EmptyAggregationContent';

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
