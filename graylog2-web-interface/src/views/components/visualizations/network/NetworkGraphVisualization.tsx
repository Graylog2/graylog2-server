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

import buildGraph from './buildGraph';

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

const runLayout = (nodeCount: number, links: ReadonlyArray<{ source: number; target: number }>): Array<SimNode> => {
  const simNodes: Array<SimNode> = Array.from({ length: nodeCount }, (_, i) => ({ id: i }));
  const simLinks: Array<SimLink> = links.map((l) => ({ source: l.source, target: l.target }));

  const simulation = forceSimulation(simNodes as never)
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
  x: Array<number | null>;
  y: Array<number | null>;
  line: { width: number; color: string };
  // Per-point customdata so plotly attaches the edge metadata to whichever point the
  // user lands on when clicking the line segment.
  customdata: Array<EdgeCustomData | null>;
  hoverinfo: 'none';
  showlegend: false;
};

type FontStyle = { color: string };

type ColorScale = Array<[number, string]>;

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
    colorscale: ColorScale;
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

const buildLayout = (width: number, height: number) => ({
  margin: { t: 20, b: 70, l: 20, r: 20 },
  xaxis: { visible: false, zeroline: false, showgrid: false, fixedrange: true, autorange: true as const },
  yaxis: { visible: false, zeroline: false, showgrid: false, fixedrange: true, autorange: true as const },
  showlegend: false,
  hovermode: 'closest' as const,
  width,
  height,
});

const NetworkGraphVisualization = makeVisualization(
  ({ config, data, height, width }: VisualizationComponentProps) => {
    const rows = retrieveChartData(data);
    const mapKeys = useMapKeys();
    const theme = useTheme();
    const { onChartClick, initializeGraphDivRef, popover } = usePlotOnClickPopover('network', config);

    const traces = useMemo<[EdgeTrace, NodeTrace] | null>(() => {
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

      const edgeX: Array<number | null> = [];
      const edgeY: Array<number | null> = [];
      const edgeCustomData: Array<EdgeCustomData | null> = [];
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

        edgeX.push(s.x, t.x, null);
        edgeY.push(s.y, t.y, null);
        // Both points carry the same edge metadata; the separator slot is `null` so
        // plotly won't surface a click between segments.
        edgeCustomData.push(cd, cd, null);
      });

      const textColor = theme.colors.text.primary;
      // Start the gradient at the saturated `info` shade — the `lightest`/`lighter` tints
      // blend into the chart background and bury low-degree nodes.
      const nodeColorscale: ColorScale = [
        [0, theme.colors.variant.info],
        [0.5, theme.colors.variant.dark.info],
        [1, theme.colors.variant.darkest.info],
      ];

      const edgeTrace: EdgeTrace = {
        type: 'scatter',
        mode: 'lines',
        x: edgeX,
        y: edgeY,
        line: { width: 1, color: theme.colors.text.secondary },
        customdata: edgeCustomData,
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
          colorscale: nodeColorscale,
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
          line: { width: 1.5, color: theme.colors.global.contentBackground },
        },
        customdata: nodes.map((n) => ({ field: n.field, value: n.value })),
        hovertemplate: '%{text}<br>Connections: %{marker.color}<extra></extra>',
        showlegend: false,
      };

      return [edgeTrace, nodeTrace];
    }, [config, mapKeys, rows, theme]);

    const layout = useMemo(() => buildLayout(width, height), [width, height]);

    return (
      <Container $height={height} $width={width}>
        {traces ? (
          <GenericPlot
            chartData={traces}
            layout={layout}
            onClickMarker={onChartClick}
            onInitialized={initializeGraphDivRef}
          />
        ) : (
          <EmptyState>No connections to display. Adjust your search or grouping to see results.</EmptyState>
        )}
        {popover}
      </Container>
    );
  },
  'network',
);

NetworkGraphVisualization.displayName = 'NetworkGraphVisualization';

export default NetworkGraphVisualization;
