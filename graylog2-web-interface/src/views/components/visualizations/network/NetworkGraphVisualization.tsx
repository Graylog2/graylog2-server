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
import { forceSimulation, forceLink, forceManyBody, forceCenter } from 'd3-force';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import extractLeafPaths from 'views/components/visualizations/utils/extractLeafPaths';
import type { NodeCustomData } from 'views/components/visualizations/sankey/SankeyVisualization';

import buildGraph from './buildGraph';
import useNetworkOnClickPopover from './useNetworkOnClickPopover';

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

const LAYOUT_ITERATIONS = 300;

const runLayout = (nodeCount: number, links: ReadonlyArray<{ source: number; target: number }>): Array<SimNode> => {
  const simNodes: Array<SimNode> = Array.from({ length: nodeCount }, (_, i) => ({ id: i }));
  const simLinks: Array<SimLink> = links.map((l) => ({ source: l.source, target: l.target }));

  const simulation = forceSimulation(simNodes as never)
    .force(
      'link',
      forceLink(simLinks as never)
        .id((d: SimNode) => d.id)
        .distance(60),
    )
    .force('charge', forceManyBody().strength(-220))
    .force('center', forceCenter(0, 0))
    .stop();

  for (let i = 0; i < LAYOUT_ITERATIONS; i += 1) simulation.tick();

  return simNodes;
};

type EdgeTrace = {
  type: 'scatter';
  mode: 'lines';
  x: Array<number | null>;
  y: Array<number | null>;
  line: { width: number; color: string };
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
  textposition: 'top center';
  textfont: FontStyle;
  marker: {
    size: number;
    color: Array<number>;
    colorscale: string;
    showscale: boolean;
    colorbar: { title: { text: string; font: FontStyle }; thickness: number; tickfont: FontStyle };
    line: { width: number; color: string };
  };
  customdata: Array<NodeCustomData>;
  hovertemplate: string;
  showlegend: false;
};

const buildLayout = (width: number, height: number) => ({
  margin: { t: 20, b: 20, l: 20, r: 20 },
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
    const { onChartClick, popover } = useNetworkOnClickPopover();

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
      edges.forEach((edge) => {
        const s = positions[edge.source];
        const t = positions[edge.target];

        if (s.x === undefined || s.y === undefined || t.x === undefined || t.y === undefined) return;

        edgeX.push(s.x, t.x, null);
        edgeY.push(s.y, t.y, null);
      });

      const textColor = theme.colors.text.primary;

      const edgeTrace: EdgeTrace = {
        type: 'scatter',
        mode: 'lines',
        x: edgeX,
        y: edgeY,
        line: { width: 1, color: theme.colors.text.secondary },
        hoverinfo: 'none',
        showlegend: false,
      };

      const displayLabels = nodes.map((n) => String(mapKeys(n.value, n.field) ?? n.label));

      const nodeTrace: NodeTrace = {
        type: 'scatter',
        mode: 'markers+text',
        x: positions.map((p) => p.x ?? 0),
        y: positions.map((p) => p.y ?? 0),
        text: displayLabels,
        textposition: 'top center',
        textfont: { color: textColor },
        marker: {
          size: 14,
          color: nodes.map((n) => n.degree),
          colorscale: 'YlGnBu',
          showscale: true,
          colorbar: {
            title: { text: 'Connections', font: { color: textColor } },
            thickness: 12,
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
          <GenericPlot chartData={traces} layout={layout} onClickMarker={onChartClick} />
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
