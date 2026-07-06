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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import * as Immutable from 'immutable';

import mockComponent from 'helpers/mocking/MockComponent';
import Pivot from 'views/logic/aggregationbuilder/Pivot';
import Series from 'views/logic/aggregationbuilder/Series';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';
import TestFieldTypesContextProvider from 'views/components/contexts/TestFieldTypesContextProvider';
import asMock from 'helpers/mocking/AsMock';
import GenericPlot from 'views/components/visualizations/GenericPlot';

import * as fixtures from './fixtures';

import SankeyVisualization from '../SankeyVisualization';

jest.mock('../../GenericPlot', () => jest.fn(mockComponent('GenericPlot')));

// Resolve the two distinct ids used by the `sameNameDifferentIds` fixture to a shared name,
// leaving every other key untouched so the remaining tests keep their identity mapping.
jest.mock('views/components/visualizations/useMapKeys', () => ({
  __esModule: true,
  default: () => (key: string) => (key === 'id-1' || key === 'id-2' ? 'Shared name' : key),
}));

const effectiveTimerange: AbsoluteTimeRange = {
  type: 'absolute',
  from: '2022-04-27T12:15:59.633Z',
  to: '2022-04-27T12:20:59.633Z',
};

const WrappedSankey = ({ ...props }: React.ComponentProps<typeof SankeyVisualization>) => (
  <TestStoreProvider>
    <TestFieldTypesContextProvider>
      <SankeyVisualization {...props} />
    </TestFieldTypesContextProvider>
  </TestStoreProvider>
);

const baseProps = {
  effectiveTimerange,
  fields: Immutable.List() as FieldTypeMappingsList,
  height: 800,
  width: 600,
  setLoadingState: () => {},
  onChange: () => {},
  toggleEdit: () => {},
};

const lastTrace = () => {
  const { calls } = asMock(GenericPlot).mock;
  const lastCall = calls[calls.length - 1];

  return lastCall[0].chartData[0];
};

describe('SankeyVisualization', () => {
  useViewsPlugin();

  beforeEach(() => {
    asMock(GenericPlot).mockClear();
  });

  it('renders nodes and links for two row groupings + one metric', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const trace = lastTrace();

    expect(trace.type).toBe('sankey');
    expect(trace.node.label).toEqual(['a1', 'b1', 'b2', 'a2']);
    expect(trace.node.customdata).toEqual([
      { field: 'a', value: 'a1' },
      { field: 'b', value: 'b1' },
      { field: 'b', value: 'b2' },
      { field: 'a', value: 'a2' },
    ]);
    expect(trace.link.source).toEqual([0, 0, 3]);
    expect(trace.link.target).toEqual([1, 2, 1]);
    expect(trace.link.value).toEqual([5, 3, 7]);
  });

  it('renders nodes and links for one row + one column pivot', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a'])])
      .columnPivots([Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.oneRowOneColPivot} />);

    const trace = lastTrace();

    expect(trace.node.label).toEqual(['a1', 'b1', 'b2', 'a2']);
    expect(trace.link.source).toEqual([0, 0, 3]);
    expect(trace.link.target).toEqual([1, 2, 1]);
    expect(trace.link.value).toEqual([5, 3, 7]);
  });

  it('produces three stages when three groupings are configured and aggregates shared prefixes', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .columnPivots([Pivot.createValues(['c'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.threeGroupings} />);

    const trace = lastTrace();
    const linksByLabel = trace.link.source.map((s: number, i: number) => ({
      from: trace.node.label[s],
      to: trace.node.label[trace.link.target[i]],
      value: trace.link.value[i],
    }));

    expect(linksByLabel).toEqual([
      { from: 'a1', to: 'b1', value: 5 },
      { from: 'b1', to: 'c1', value: 2 },
      { from: 'b1', to: 'c2', value: 9 },
      { from: 'a1', to: 'b2', value: 4 },
      { from: 'b2', to: 'c1', value: 4 },
      { from: 'a2', to: 'b1', value: 6 },
    ]);
  });

  it('keeps repeated labels as distinct nodes per stage (no self-cycle)', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.repeatedLabels} />);

    const trace = lastTrace();

    expect(trace.node.label).toEqual(['x', 'x', 'y']);
    expect(trace.link.source).toEqual([0, 2]);
    expect(trace.link.target).toEqual([1, 1]);
    expect(trace.link.value).toEqual([4, 2]);
  });

  it('keeps distinct ids that resolve to the same name as separate nodes', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['stream']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.sameNameDifferentIds} />);

    const trace = lastTrace();

    expect(trace.node.label).toEqual(['Shared name', 'b1', 'Shared name']);
    expect(trace.node.customdata).toEqual([
      { field: 'stream', value: 'id-1' },
      { field: 'b', value: 'b1' },
      { field: 'stream', value: 'id-2' },
    ]);
    expect(trace.link.source).toEqual([0, 2]);
    expect(trace.link.target).toEqual([1, 1]);
    expect(trace.link.value).toEqual([5, 3]);
  });

  it('drops links with null, zero, or negative metric values', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.droppableValues} />);

    const trace = lastTrace();

    expect(trace.node.label).toEqual(['a1', 'b1']);
    expect(trace.link.source).toEqual([0]);
    expect(trace.link.target).toEqual([1]);
    expect(trace.link.value).toEqual([5]);
  });

  it('uses a static weight of 1 per link when no metric is configured', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.twoRowPivotsNoMetric} />);

    const trace = lastTrace();

    expect(trace.node.label).toEqual(['a1', 'b1', 'b2', 'a2']);
    expect(trace.link.source).toEqual([0, 0, 3]);
    expect(trace.link.target).toEqual([1, 2, 1]);
    expect(trace.link.value).toEqual([1, 1, 1]);
  });

  it('themes the link color for visibility while leaving node colors to the default palette', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const trace = lastTrace();

    expect(typeof trace.link.color).toBe('string');
    expect(trace.link.color).not.toHaveLength(0);
    expect(trace.node.color).toBeUndefined();
  });

  it('uses a distinct, more prominent hover color so hovered links stand out', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const trace = lastTrace();

    expect(typeof trace.link.hovercolor).toBe('string');
    expect(trace.link.hovercolor).not.toHaveLength(0);
    // The hover color differs from the resting color, so hovering changes the appearance.
    expect(trace.link.hovercolor).not.toEqual(trace.link.color);
  });

  it('renders an empty-state message when there are no rows', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .series([Series.forFunction('count()')])
      .visualization('sankey')
      .build();

    render(<WrappedSankey {...baseProps} config={config} data={{ chart: [] }} />);

    expect(screen.getByText(/No flows to display/i)).toBeInTheDocument();
    expect(GenericPlot).not.toHaveBeenCalled();
  });
});
