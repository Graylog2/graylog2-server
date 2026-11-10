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
import styled, { css } from 'styled-components';
import chroma from 'chroma-js';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization, retrieveChartData } from 'views/components/aggregationbuilder/AggregationBuilder';
import type { Key } from 'views/logic/searchtypes/pivot/PivotHandler';
import useMapKeys from 'views/components/visualizations/useMapKeys';
import type { LeafPath } from 'views/components/visualizations/utils/extractLeafPaths';
import extractLeafPaths from 'views/components/visualizations/utils/extractLeafPaths';
import usePlotOnClickPopover from 'views/components/visualizations/hooks/usePlotOnClickPopover';
import sankeyOnClickPopover from 'views/components/visualizations/sankey/sankeyOnClickPopover';
import { defaultChartColors } from 'views/components/visualizations/Colors';

import { SANKEY_VISUALIZATION_TYPE } from './Constants';

import GenericPlot from '../GenericPlot';

// Fill the parent fluidly (rather than pinning to the height/width props) so the chart tracks
// container resizes via CSS in both view and edit mode; plotly's `responsive` handles the redraw.
const Container = styled.div`
  height: 100%;
  width: 100%;
`;

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

export type NodeCustomData = { field: string; value: Key };

export type SankeyTrace = {
  type: typeof SANKEY_VISUALIZATION_TYPE;
  orientation: 'h';
  arrangement: 'fixed';
  node: {
    label: Array<string>;
    customdata: Array<NodeCustomData>;
    color: Array<string>;
    pad: number;
    thickness: number;
  };
  link: {
    source: Array<number>;
    target: Array<number>;
    value: Array<number>;
    label: Array<string>;
    color: Array<string>;
    hovercolor: Array<string>;
  };
};

const STAGE_SEPARATOR = ' ';

const NODE_ALPHA = 1;
const LINK_ALPHA = 0.3;
const LINK_HOVER_ALPHA = 0.6;

// Hex notation, because plotly's color parser drops the alpha of the `rgb(r g b / a)` syntax chroma emits by default.
const paletteColor = (index: number, alpha: number) =>
  chroma(defaultChartColors[index % defaultChartColors.length])
    .alpha(alpha)
    .hex('rgba');

const buildSankeyTrace = (
  paths: Array<LeafPath>,
  displayKeys: Array<Array<string>>,
  allFields: Array<string>,
): SankeyTrace => {
  const nodeIndex = new Map<string, number>();
  const labels: Array<string> = [];
  const customdata: Array<NodeCustomData> = [];

  const indexOf = (stage: number, label: string, originalValue: Key): number => {
    // Key by the original value (e.g. a stream/input/node id) rather than the resolved
    // label, so distinct ids that happen to resolve to the same name stay separate nodes.
    const k = `${stage}${STAGE_SEPARATOR}${String(originalValue)}`;
    const existing = nodeIndex.get(k);

    if (existing !== undefined) return existing;

    const idx = labels.length;
    nodeIndex.set(k, idx);
    labels.push(label);
    customdata.push({ field: allFields[stage], value: originalValue });

    return idx;
  };

  const linkAgg = new Map<string, { source: number; target: number; value: number }>();

  paths.forEach((path, pathIdx) => {
    const displayed = displayKeys[pathIdx];

    for (let i = 0; i < displayed.length - 1; i += 1) {
      const source = indexOf(i, displayed[i], path.keys[i]);
      const target = indexOf(i + 1, displayed[i + 1], path.keys[i + 1]);
      const linkKey = `${source}${STAGE_SEPARATOR}${target}`;
      const existing = linkAgg.get(linkKey);

      if (existing) {
        existing.value += path.value;
      } else {
        linkAgg.set(linkKey, { source, target, value: path.value });
      }
    }
  });

  const source: Array<number> = [];
  const target: Array<number> = [];
  const value: Array<number> = [];
  const label: Array<string> = [];

  linkAgg.forEach(({ source: s, target: t, value: v }) => {
    source.push(s);
    target.push(t);
    value.push(v);
    label.push(`${labels[s]} → ${labels[t]}`);
  });

  return {
    type: SANKEY_VISUALIZATION_TYPE,
    orientation: 'h',
    arrangement: 'fixed',
    node: {
      label: labels,
      customdata,
      color: labels.map((_, i) => paletteColor(i, NODE_ALPHA)),
      pad: 15,
      thickness: 18,
    },
    link: {
      source,
      target,
      value,
      label,
      color: target.map((t) => paletteColor(t, LINK_ALPHA)),
      hovercolor: target.map((t) => paletteColor(t, LINK_HOVER_ALPHA)),
    },
  };
};

const layout = { margin: { t: 20, b: 20, l: 20, r: 20 } };

const SankeyVisualization = makeVisualization(({ config, data }: VisualizationComponentProps) => {
  const rows = retrieveChartData(data);
  const mapKeys = useMapKeys();
  const { onChartClick, initializeGraphDivRef, popover } = usePlotOnClickPopover({ ...sankeyOnClickPopover, config });

  const trace = useMemo<SankeyTrace | null>(() => {
    const rowFields = config.rowPivots.flatMap((pivot) => pivot.fields);
    const columnFields = config.columnPivots.flatMap((pivot) => pivot.fields);
    const allFields = [...rowFields, ...columnFields];

    if (allFields.length < 2 || !rows) {
      return null;
    }

    const metric = config.series?.[0];
    const paths = extractLeafPaths(rows, columnFields.length, metric?.effectiveName);

    if (paths.length === 0) {
      return null;
    }

    const displayKeys = paths.map((path) => path.keys.map((k, i) => String(mapKeys(k, allFields[i]) ?? k)));

    return buildSankeyTrace(paths, displayKeys, allFields);
  }, [config, mapKeys, rows]);

  return (
    <Container>
      {trace ? (
        <GenericPlot
          chartData={[trace]}
          layout={layout}
          onClickMarker={onChartClick}
          onInitialized={initializeGraphDivRef}
        />
      ) : (
        <EmptyState>No flows to display. Adjust your search or grouping to see results.</EmptyState>
      )}
      {popover}
    </Container>
  );
}, SANKEY_VISUALIZATION_TYPE);

SankeyVisualization.displayName = 'SankeyVisualization';

export default SankeyVisualization;
