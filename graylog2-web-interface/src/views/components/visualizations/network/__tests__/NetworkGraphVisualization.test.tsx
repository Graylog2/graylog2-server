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

import NetworkGraphVisualization from '../NetworkGraphVisualization';

jest.mock('../../GenericPlot', () => jest.fn(mockComponent('GenericPlot')));

const effectiveTimerange: AbsoluteTimeRange = {
  type: 'absolute',
  from: '2022-04-27T12:15:59.633Z',
  to: '2022-04-27T12:20:59.633Z',
};

const WrappedNetwork = ({ ...props }: React.ComponentProps<typeof NetworkGraphVisualization>) => (
  <TestStoreProvider>
    <TestFieldTypesContextProvider>
      <NetworkGraphVisualization {...props} />
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

const lastTraces = () => {
  const { calls } = asMock(GenericPlot).mock;
  const lastCall = calls[calls.length - 1];

  return lastCall[0].chartData as [Record<string, any>, Record<string, any>];
};

describe('NetworkGraphVisualization', () => {
  useViewsPlugin();

  beforeEach(() => {
    asMock(GenericPlot).mockClear();
  });

  it('renders edge and node traces for two row pivots + one metric', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const [edgeTrace, nodeTrace] = lastTraces();

    expect(edgeTrace.type).toBe('scatter');
    expect(edgeTrace.mode).toBe('lines');
    expect(edgeTrace.x).toHaveLength(9);
    expect(edgeTrace.y).toHaveLength(9);
    expect(edgeTrace.x[2]).toBeNull();
    expect(edgeTrace.x[5]).toBeNull();
    expect(edgeTrace.x[8]).toBeNull();

    expect(nodeTrace.type).toBe('scatter');
    expect(nodeTrace.mode).toBe('markers+text');
    expect(nodeTrace.text).toEqual(['a1', 'b1', 'b2', 'a2']);
    expect(nodeTrace.customdata).toEqual([
      { field: 'source', value: 'a1' },
      { field: 'target', value: 'b1' },
      { field: 'target', value: 'b2' },
      { field: 'source', value: 'a2' },
    ]);
    expect(nodeTrace.marker.color).toEqual([2, 2, 1, 1]);
  });

  it('unifies same value across stages into a single node', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.sharedValue} />);

    const [, nodeTrace] = lastTraces();

    expect(nodeTrace.text).toEqual(['x', 'y']);
    expect(nodeTrace.marker.color).toEqual([2, 2]);
  });

  it('uses static weight of 1 per edge when no metric is configured', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivotsNoMetric} />);

    const [, nodeTrace] = lastTraces();

    expect(nodeTrace.text).toEqual(['a1', 'b1', 'a2', 'b2']);
    expect(nodeTrace.marker.color).toEqual([1, 1, 1, 1]);
  });

  it('chains edges across 3 groupings', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .columnPivots([Pivot.createValues(['c'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.threeGroupings} />);

    const [edgeTrace, nodeTrace] = lastTraces();

    expect(nodeTrace.text).toEqual(['a1', 'b1', 'c1', 'c2', 'b2']);
    // 5 edges × 3 entries (src, tgt, null) = 15
    expect(edgeTrace.x).toHaveLength(15);
    expect(nodeTrace.customdata).toEqual([
      { field: 'a', value: 'a1' },
      { field: 'b', value: 'b1' },
      { field: 'c', value: 'c1' },
      { field: 'c', value: 'c2' },
      { field: 'b', value: 'b2' },
    ]);
  });

  it('renders an empty-state message when there are no edges', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={{ chart: [] }} />);

    expect(screen.getByText(/No connections to display/i)).toBeInTheDocument();
    expect(GenericPlot).not.toHaveBeenCalled();
  });
});
