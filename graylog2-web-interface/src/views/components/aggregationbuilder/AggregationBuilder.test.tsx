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
import * as Immutable from 'immutable';

import Pivot from 'views/logic/aggregationbuilder/Pivot';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { Result, RowInner } from 'views/logic/searchtypes/pivot/PivotHandler';

import OriginalAggregationBuilder from './AggregationBuilder';
import EmptyAggregationContent from './EmptyAggregationContent';

import OnVisualizationConfigChangeContext from '../aggregationwizard/OnVisualizationConfigChangeContext';

const mockDummyVisualization = () => 'dummy-visualization';

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: {
    exports: () => [
      {
        type: 'dummy',
        displayName: 'Some Dummy Visualization',
        component: mockDummyVisualization,
      },
    ],
  },
}));

type OriginalProps = React.ComponentProps<typeof OriginalAggregationBuilder>;
const AggregationBuilder = ({
  data,
  editing = false,
  ...props
}: {
  config: OriginalProps['config'];
  editing?: OriginalProps['editing'];
  data: { [_key: string]: Partial<Result> };
}) => (
  <OriginalAggregationBuilder
    id="test-widget"
    editing={editing}
    queryId="foobar"
    width={640}
    height={480}
    setLoadingState={() => {}}
    fields={Immutable.List()}
    data={data as OriginalProps['data']}
    {...props}
  />
);

const dataPoint: RowInner = { value: 3.1415926, source: 'row-inner', key: ['pi'], rollup: false };

describe('AggregationBuilder', () => {
  const rowPivot = Pivot.createValues(['field']);

  it('does render empty result widget when no documents were in result and is edit', () => {
    const wrapper = mount(
      <AggregationBuilder
        data={{ chart: { total: 0 } }}
        editing
        config={AggregationWidgetConfig.builder().visualization('dummy').build()}
      />,
    );

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(1);
    expect(wrapper.find(EmptyAggregationContent)).toHaveProp('editing', true);
  });

  it('renders dummy component with rows from data', () => {
    const wrapper = mount(
      <AggregationBuilder
        config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
        data={{ chart: { total: 42, rows: [dataPoint] } }}
      />,
    );

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(0);
    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(0);

    const dummyVisualization = wrapper.find(mockDummyVisualization);

    expect(dummyVisualization).toHaveLength(1);
    expect(dummyVisualization).toHaveProp('data', {
      chart: [{ value: 3.1415926, source: 'row-inner', key: ['pi'], rollup: false }],
    });
  });

  it('passes through onVisualizationConfigChange to visualization', () => {
    const onVisualizationConfigChange = jest.fn();
    const wrapper = mount(
      <OnVisualizationConfigChangeContext.Provider value={onVisualizationConfigChange}>
        <AggregationBuilder
          config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
          data={{ chart: { total: 42, rows: [{ value: 3.1415926, source: 'row-inner', key: ['pi'], rollup: false }] } }}
        />
      </OnVisualizationConfigChangeContext.Provider>,
    );

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(0);

    const dummyVisualization = wrapper.find(mockDummyVisualization);

    expect(dummyVisualization).toHaveProp('onChange', onVisualizationConfigChange);
  });

  it('renders EmptyAggregationContent if the AggregationWidgetConfig is empty', () => {
    const wrapper = mount(
      <AggregationBuilder
        config={AggregationWidgetConfig.builder().visualization('dummy').build()}
        data={{ chart: { total: 42, rows: [dataPoint] } }}
      />,
    );

    expect(wrapper.find(EmptyAggregationContent)).toHaveLength(1);
    expect(wrapper.find(EmptyAggregationContent)).toHaveProp('editing', false);
  });

  it('falls back to retrieving effective timerange from first result if no `chart` result present', () => {
    const data = {
      '524d182c-8e32-4372-b30d-a40d99efe55d': {
        total: 42,
        rows: [dataPoint],
        effective_timerange: 42,
      },
    };
    const wrapper = mount(
      <AggregationBuilder
        config={AggregationWidgetConfig.builder().rowPivots([rowPivot]).visualization('dummy').build()}
        data={data}
      />,
    );
    const dummyVisualization = wrapper.find(mockDummyVisualization);

    expect(dummyVisualization).toHaveProp('effectiveTimerange', 42);
  });
});
