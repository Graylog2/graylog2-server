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
import { useMemo } from 'react';
import styled, { css, useTheme } from 'styled-components';
import { forceSimulation, forceLink, forceManyBody, forceCenter, forceCollide } from 'd3-force';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import extractLeafPaths from 'views/components/visualizations/utils/extractLeafPaths';
import type { NodeCustomData } from 'views/components/visualizations/sankey/SankeyVisualization';
import usePlotOnClickPopover from 'views/components/visualizations/hooks/usePlotOnClickPopover';
import NetworkVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NetworkVisualizationConfig';

import buildGraph from './buildGraph';
import edgeColorScale from './edgeColorScale';
import normalizeEdgeValue from './normalizeEdgeValue';
import networkOnClickPopover from './networkOnClickPopover';

import GenericPlot from '../GenericPlot';

const Container = styled.div<{ $height: number; $width: number }>(
  ({ $height, $width }) => css`
    height: ${$height ? `${$height}px` : '100%'};
    width: ${$width ? `${$width}px` : '100%'};
  `,
);

const EmptyState = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    width: 100%;
    padding: ${theme.spacings.md};
    color: ${theme.colors.text.secondary};
    text-align: center;
  `,
);

type SimNode = { id: number; x?: number; y?: number };
type SimLink = { source: number | SimNode; target: number | SimNode };

const LAYOUT_ITERATIONS = 500;
const NODE_RADIUS = 75;
// Number of invisible hit-target markers sampled along each edge so the whole edge is clickable.
// Plotly click detection is point-based, and an edge's only real data points are its endpoints
// (which coincide with node markers), so without these an edge line is not clickable.
const EDGE_HIT_SAMPLES = 9;
// Edge weight is encoded through the colorscale, not thickness, so every edge shares one width.
const EDGE_WIDTH = 2;

const runLayout = (nodeCount: number, links: ReadonlyArray<{ source: number; target: number }>): Array<SimNode> => {
  const simNodes: Array<SimNode> = Array.from({ length: nodeCount }, (_, i) => ({ id: i }));
  const simLinks: Array<SimLink> = links.map((l) => ({ source: l.source, target: l.target }));

  const simulation = forceSimulation(simNodes)
    .force(
      'link',
      forceLink(simLinks as never)
        .id((d: SimNode) => d.id)
        .distance(200),
    )
    .force('charge', forceManyBody().strength(-900))
    .force('collide', forceCollide(NODE_RADIUS))
    .force('center', forceCenter(0, 0))
    .stop();

  for (let i = 0; i < LAYOUT_ITERATIONS; i += 1) simulation.tick();

  return simNodes;
};

type TextPosition =
  | 'top left'
  | 'top center'
  | 'top right'
  | 'middle left'
  | 'middle right'
  | 'bottom left'
  | 'bottom center'
  | 'bottom right';

const radialTextPosition = (x: number, y: number): TextPosition => {
  if (x === 0 && y === 0) return 'top center';

  // Plotly's y axis points up, so y > 0 is visually "top".
  const angle = Math.atan2(y, x);
  const slice = Math.round((angle / Math.PI) * 4);

  switch (((slice % 8) + 8) % 8) {
    case 0:
      return 'middle right';
    case 1:
      return 'top right';
    case 2:
      return 'top center';
    case 3:
      return 'top left';
    case 4:
      return 'middle left';
    case 5:
      return 'bottom left';
    case 6:
      return 'bottom center';
    case 7:
      return 'bottom right';
    default:
      return 'top center';
  }
};

type EdgeEndpoint = { customdata: NodeCustomData; label: string };
type EdgeCustomData = { source: EdgeEndpoint; target: EdgeEndpoint; value: number };

type EdgeTrace = {
  type: 'scatter';
  mode: 'lines';
  x: Array<number>;
  y: Array<number>;
  line: { width: number; color: string };
  // Both endpoints carry the same edge metadata so plotly attaches it wherever
  // the user clicks along the segment.
  customdata: Array<EdgeCustomData>;
  hoverinfo: 'none';
  showlegend: false;
};

type HitTrace = {
  type: 'scatter';
  mode: 'markers';
  x: Array<number>;
  y: Array<number>;
  marker: { size: number; opacity: number; color: string };
  customdata: Array<EdgeCustomData>;
  hoverinfo: 'none';
  showlegend: false;
};

type FontStyle = { color: string };

type NodeTrace = {
  type: 'scatter';
  mode: 'markers+text';
  x: Array<number>;
  y: Array<number>;
  text: Array<string>;
  textposition: Array<TextPosition>;
  textfont: FontStyle;
  cliponaxis: false;
  marker: {
    size: number;
    color: Array<number>;
    colorscale: string;
    reversescale: boolean;
    showscale: boolean;
    colorbar: {
      title: { text: string; font: FontStyle; side: 'top' };
      orientation: 'h';
      x: number;
      xanchor: 'center';
      y: number;
      yanchor: 'top';
      len: number;
      thickness: number;
      tickfont: FontStyle;
    };
    line: { width: number; color: string };
  };
  customdata: Array<NodeCustomData>;
  hovertemplate: string;
  showlegend: false;
};

// Wheel zooms toward the cursor; double-click resets to the initial view. Drag pans (see `dragmode`).
// Zoom only takes effect in interactive contexts — elsewhere GenericPlot forces `fixedrange: true`.
const PLOT_CONFIG = { scrollZoom: true, doubleClick: 'reset' } as const;

// Fraction of the node span reserved on each side of the initial/reset view so the outward-pointing
// labels aren't clipped at the canvas edge. Users can still zoom and pan past it.
const RANGE_PADDING = 0.2;

type NodePositions = { xs: Array<number>; ys: Array<number> };

const paddedRange = (values: ReadonlyArray<number>): [number, number] => {
  const min = Math.min(...values);
  const max = Math.max(...values);
  const span = max - min || 1;
  const pad = span * RANGE_PADDING;

  return [min - pad, max + pad];
};

const buildLayout = (width: number, height: number, positions: NodePositions | null) => {
  const base = {
    margin: { t: 20, b: 70, l: 20, r: 20 },
    dragmode: 'pan' as const,
    xaxis: { visible: false, zeroline: false, showgrid: false, fixedrange: false },
    yaxis: { visible: false, zeroline: false, showgrid: false, fixedrange: false },
    showlegend: false,
    hovermode: 'closest' as const,
    width,
    height,
  };

  if (!positions) {
    return {
      ...base,
      xaxis: { ...base.xaxis, autorange: true as const },
      yaxis: { ...base.yaxis, autorange: true as const },
    };
  }

  // Explicit padded ranges (rather than autorange) so the reserved margin survives a double-click
  // reset, which restores the range the chart was first rendered with.
  return {
    ...base,
    xaxis: { ...base.xaxis, range: paddedRange(positions.xs) },
    yaxis: { ...base.yaxis, range: paddedRange(positions.ys) },
  };
};

const NetworkGraphVisualization = makeVisualization(({ config, data, height, width }: VisualizationComponentProps) => {
  const rows = retrieveChartData(data);
  const mapKeys = useMapKeys();
  const theme = useTheme();
  const { onChartClick, initializeGraphDivRef, popover } = usePlotOnClickPopover({ ...networkOnClickPopover, config });
  const visualizationConfig =
    (config.visualizationConfig as NetworkVisualizationConfig) ?? NetworkVisualizationConfig.empty();

  const plot = useMemo<{
    traces: [...Array<EdgeTrace>, HitTrace, NodeTrace];
    xs: Array<number>;
    ys: Array<number>;
  } | null>(() => {
    const rowFields = config.rowPivots.flatMap((pivot) => pivot.fields);
    const columnFields = config.columnPivots.flatMap((pivot) => pivot.fields);
    const allFields = [...rowFields, ...columnFields];

    if (allFields.length < 2 || !rows) return null;

    const metric = config.series?.[0];
    const paths = extractLeafPaths(rows, columnFields.length, metric?.effectiveName);

    if (paths.length === 0) return null;

    const { nodes, edges } = buildGraph(paths, allFields);

    if (edges.length === 0) return null;

    const positions = runLayout(nodes.length, edges);

    const values = edges.map((edge) => edge.value);
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);
    // Edge weight is encoded through the configured node colorscale: each edge's aggregated value is
    // normalized to [0, 1] and sampled from the same scale, honouring the reverse-scale option.
    const scale = edgeColorScale(visualizationConfig.colorScale);

    // Plotly's `line.color` is per-trace, so each edge is its own two-point line trace to let its
    // color track the metric value.
    const edgeTraces: Array<EdgeTrace> = [];
    // Invisible hit-target markers sampled along every edge, collected into one trace, so clicks
    // anywhere along an edge are detectable (see EDGE_HIT_SAMPLES).
    const hitX: Array<number> = [];
    const hitY: Array<number> = [];
    const hitCustomData: Array<EdgeCustomData> = [];
    edges.forEach((edge) => {
      const s = positions[edge.source];
      const t = positions[edge.target];

      if (s.x === undefined || s.y === undefined || t.x === undefined || t.y === undefined) return;

      const sourceNode = nodes[edge.source];
      const targetNode = nodes[edge.target];
      const cd: EdgeCustomData = {
        source: {
          customdata: { field: sourceNode.field, value: sourceNode.value },
          label: sourceNode.label,
        },
        target: {
          customdata: { field: targetNode.field, value: targetNode.value },
          label: targetNode.label,
        },
        value: edge.value,
      };

      const normalized = normalizeEdgeValue(edge.value, minValue, maxValue);
      const position = visualizationConfig.reverseScale ? 1 - normalized : normalized;

      edgeTraces.push({
        type: 'scatter',
        mode: 'lines',
        x: [s.x, t.x],
        y: [s.y, t.y],
        line: { width: EDGE_WIDTH, color: scale(position).hex() },
        customdata: [cd, cd],
        hoverinfo: 'none',
        showlegend: false,
      });

      // Sample interior points along the edge (excluding endpoints, which sit on the nodes) so a
      // click anywhere along the edge lands on a hit-detectable marker carrying the edge metadata.
      for (let i = 1; i <= EDGE_HIT_SAMPLES; i += 1) {
        const f = i / (EDGE_HIT_SAMPLES + 1);
        hitX.push(s.x + f * (t.x - s.x));
        hitY.push(s.y + f * (t.y - s.y));
        hitCustomData.push(cd);
      }
    });

    const textColor = theme.colors.text.primary;

    const hitTrace: HitTrace = {
      type: 'scatter',
      mode: 'markers',
      x: hitX,
      y: hitY,
      // Invisible (opacity 0) but still rendered as points, so they remain hit-detectable.
      marker: { size: 10, opacity: 0, color: theme.colors.text.secondary },
      customdata: hitCustomData,
      hoverinfo: 'none',
      showlegend: false,
    };

    const displayLabels = nodes.map((n) => String(mapKeys(n.value, n.field) ?? n.label));

    const xs = positions.map((p) => p.x ?? 0);
    const ys = positions.map((p) => p.y ?? 0);

    const nodeTrace: NodeTrace = {
      type: 'scatter',
      mode: 'markers+text',
      x: xs,
      y: ys,
      text: displayLabels,
      textposition: positions.map((p) => radialTextPosition(p.x ?? 0, p.y ?? 0)),
      textfont: { color: textColor },
      cliponaxis: false,
      marker: {
        size: 14,
        color: nodes.map((n) => n.degree),
        colorscale: visualizationConfig.colorScale,
        reversescale: visualizationConfig.reverseScale,
        showscale: true,
        colorbar: {
          title: { text: 'Connections', font: { color: textColor }, side: 'top' },
          orientation: 'h',
          x: 0.5,
          xanchor: 'center',
          y: -0.05,
          yanchor: 'top',
          len: 0.5,
          thickness: 10,
          tickfont: { color: textColor },
        },
        line: { width: 1.5, color: theme.colors.text.primary },
      },
      customdata: nodes.map((n) => ({ field: n.field, value: n.value })),
      hovertemplate: '%{text}<br>Connections: %{marker.color}<extra></extra>',
      showlegend: false,
    };

    return { traces: [...edgeTraces, hitTrace, nodeTrace], xs, ys };
  }, [config, mapKeys, rows, theme, visualizationConfig]);

  const layout = useMemo(() => buildLayout(width, height, plot), [width, height, plot]);

  return (
    <Container $height={height} $width={width}>
      {plot ? (
        <GenericPlot
          chartData={plot.traces}
          layout={layout}
          config={PLOT_CONFIG}
          onClickMarker={onChartClick}
          onInitialized={initializeGraphDivRef}
        />
      ) : (
        <EmptyState>No connections to display. Adjust your search or grouping to see results.</EmptyState>
      )}
      {popover}
    </Container>
  );
}, 'network');

NetworkGraphVisualization.displayName = 'NetworkGraphVisualization';

export default NetworkGraphVisualization;
