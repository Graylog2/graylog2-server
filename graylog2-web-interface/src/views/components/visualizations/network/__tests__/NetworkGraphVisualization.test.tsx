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
import NetworkVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NetworkVisualizationConfig';
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

const chartData = (): Array<Record<string, any>> => {
  const { calls } = asMock(GenericPlot).mock;
  const lastCall = calls[calls.length - 1];

  return lastCall[0].chartData as Array<Record<string, any>>;
};

// Traces are laid out as [...edgeTraces, hitTrace, nodeTrace].
const nodeTrace = () => {
  const data = chartData();

  return data[data.length - 1];
};

const hitTrace = () => {
  const data = chartData();

  return data[data.length - 2];
};

const edgeTraces = () => chartData().slice(0, -2);

// Each edge trace carries its aggregated value in customdata, so we can look up the color assigned
// to a specific edge value.
const colorForValue = (value: number) => edgeTraces().find((edge) => edge.customdata[0].value === value)?.line.color;

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

    const edges = edgeTraces();
    const node = nodeTrace();

    // 3 edges, each its own two-point line trace.
    expect(edges).toHaveLength(3);
    edges.forEach((edge) => {
      expect(edge.type).toBe('scatter');
      expect(edge.mode).toBe('lines');
      expect(edge.x).toHaveLength(2);
      expect(edge.y).toHaveLength(2);
    });

    expect(node.type).toBe('scatter');
    expect(node.mode).toBe('markers+text');
    expect(node.text).toEqual(['a1', 'b1', 'b2', 'a2']);
    expect(node.customdata).toEqual([
      { field: 'source', value: 'a1' },
      { field: 'target', value: 'b1' },
      { field: 'target', value: 'b2' },
      { field: 'source', value: 'a2' },
    ]);
    expect(node.marker.color).toEqual([2, 2, 1, 1]);
  });

  it('renders an invisible hit-target trace with sampled points along each edge', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const hits = hitTrace();

    // 3 edges × 9 samples each.
    expect(hits.type).toBe('scatter');
    expect(hits.mode).toBe('markers');
    expect(hits.marker.opacity).toBe(0);
    expect(hits.x).toHaveLength(27);
    expect(hits.y).toHaveLength(27);
    // Each sample carries the edge (link) metadata so the popover treats it as an edge.
    expect(hits.customdata[0].source).toBeDefined();
    expect(hits.customdata[0].target).toBeDefined();

    // The node trace stays last so it wins hit-detection near endpoints.
    expect(nodeTrace().mode).toBe('markers+text');
  });

  it('colors edges by the aggregated metric value via the colorscale, at a uniform width', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    // All edges share one width; weight is conveyed by color.
    edgeTraces().forEach((edge) => {
      expect(edge.line.width).toBe(2);
      expect(edge.line.color).toMatch(/^#[0-9a-f]{6}$/i);
    });

    // twoRowPivots has distinct edge values (3, 5, 7), so the low and high edges sample different
    // ends of the colorscale.
    expect(colorForValue(3)).not.toBe(colorForValue(7));
  });

  it('reverses the color mapping when reverseScale is enabled', () => {
    const buildConfig = (reverseScale: boolean) =>
      AggregationWidgetConfig.builder()
        .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
        .series([Series.forFunction('count()')])
        .visualization('network')
        .visualizationConfig(NetworkVisualizationConfig.create('YlOrRd', reverseScale))
        .build();

    render(<WrappedNetwork {...baseProps} config={buildConfig(false)} data={fixtures.twoRowPivots} />);
    const forwardMax = colorForValue(7);

    asMock(GenericPlot).mockClear();
    render(<WrappedNetwork {...baseProps} config={buildConfig(true)} data={fixtures.twoRowPivots} />);

    // With the scale reversed, the max value samples the opposite end, so its color changes.
    expect(colorForValue(7)).not.toBe(forwardMax);
  });

  it('makes the graph zoomable and pannable', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const { calls } = asMock(GenericPlot).mock;
    const props = calls[calls.length - 1][0];

    expect(props.config).toEqual({ scrollZoom: true, doubleClick: 'reset' });
    expect(props.layout.dragmode).toBe('pan');
    expect(props.layout.xaxis.fixedrange).toBe(false);
    expect(props.layout.yaxis.fixedrange).toBe(false);
  });

  it('reserves padding around the nodes for the initial/reset view', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivots} />);

    const { calls } = asMock(GenericPlot).mock;
    const props = calls[calls.length - 1][0];
    const node = props.chartData[props.chartData.length - 1];
    const [xLo, xHi] = props.layout.xaxis.range;
    const [yLo, yHi] = props.layout.yaxis.range;

    // The initial range is an explicit padded range (so double-click reset restores it) that fully
    // contains every node with room to spare for the labels.
    expect(xLo).toBeLessThan(Math.min(...node.x));
    expect(xHi).toBeGreaterThan(Math.max(...node.x));
    expect(yLo).toBeLessThan(Math.min(...node.y));
    expect(yHi).toBeGreaterThan(Math.max(...node.y));
  });

  it('unifies same value across stages into a single node', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.sharedValue} />);

    const node = nodeTrace();

    expect(node.text).toEqual(['x', 'y']);
    expect(node.marker.color).toEqual([2, 2]);
  });

  it('uses static weight of 1 per edge when no metric is configured', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['source']), Pivot.createValues(['target'])])
      .series([])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.twoRowPivotsNoMetric} />);

    const node = nodeTrace();

    expect(node.text).toEqual(['a1', 'b1', 'a2', 'b2']);
    expect(node.marker.color).toEqual([1, 1, 1, 1]);
  });

  it('chains edges across 3 groupings', () => {
    const config = AggregationWidgetConfig.builder()
      .rowPivots([Pivot.createValues(['a']), Pivot.createValues(['b'])])
      .columnPivots([Pivot.createValues(['c'])])
      .series([Series.forFunction('count()')])
      .visualization('network')
      .build();

    render(<WrappedNetwork {...baseProps} config={config} data={fixtures.threeGroupings} />);

    const node = nodeTrace();

    expect(node.text).toEqual(['a1', 'b1', 'c1', 'c2', 'b2']);
    // 5 edges → 5 traces, each a two-point segment.
    expect(edgeTraces()).toHaveLength(5);
    expect(node.customdata).toEqual([
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
