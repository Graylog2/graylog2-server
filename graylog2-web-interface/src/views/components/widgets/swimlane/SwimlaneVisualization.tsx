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
import { useMemo, useRef } from 'react';
import styled, { css, useTheme } from 'styled-components';
import chroma from 'chroma-js';

import type { WidgetComponentProps } from 'views/types';
import type { MessageListResult } from 'views/components/widgets/MessageList';
import type { BackendMessage } from 'views/components/messagelist/Types';
import type SwimlaneWidgetConfig from 'views/logic/widgets/SwimlaneWidgetConfig';
import Tooltip from 'components/common/Tooltip';
import useSwimlaneClickPopover from 'views/components/widgets/swimlane/useSwimlaneClickPopover';
import useSwimlaneTimeNav from 'views/components/widgets/swimlane/useSwimlaneTimeNav';
import useSwimlaneDetailDrawer from 'views/components/widgets/swimlane/useSwimlaneDetailDrawer';
import type { ShapeName } from 'views/components/widgets/swimlane/swimlaneShapes';
import { buildShapeIndex, renderDot } from 'views/components/widgets/swimlane/swimlaneShapes';

const LANE_HEIGHT = 36;
const GROUP_HEADER_HEIGHT = 24;
const DOT_RADIUS = 5;
const LABEL_WIDTH = 160;
const AXIS_HEIGHT = 24;
const PADDING = 8;

const Wrapper = styled.div`
  display: flex;
  flex-direction: row;
  height: 100%;
  width: 100%;
  overflow: hidden;
`;

const SwimlaneColumn = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  overflow: hidden;
`;

const Banner = styled.div(
  ({ theme }) => css`
    padding: 4px ${theme.spacings.sm};
    background: ${theme.colors.variant.warning};
    color: ${theme.colors.variant.darkest.warning};
    font-size: ${theme.fonts.size.small};
    flex-shrink: 0;
  `,
);

const ZoomBanner = styled.div(
  ({ theme }) => css`
    padding: 4px ${theme.spacings.sm};
    background: ${theme.colors.variant.lightest.info};
    color: ${theme.colors.variant.darkest.info};
    font-size: ${theme.fonts.size.small};
    flex-shrink: 0;
  `,
);

const EmptyState = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: ${theme.colors.text.secondary};
  `,
);

type LaneEvent = {
  ts: number;
  colorValue: string | undefined;
  shapeValue: string | undefined;
  labelValue: string | undefined;
  correlationValue: string | undefined;
  fields: Record<string, unknown>;
  backendMessage: BackendMessage;
};

type Lane = {
  key: string;
  events: Array<LaneEvent>;
};

type RenderItem =
  | { type: 'header'; primaryValue: string; primaryField: string; y: number }
  | { type: 'lane'; lane: Lane; labelText: string; labelField: string; labelValue: string; y: number; rowIndex: number };

const formatTs = (ts: number): string => new Date(ts).toISOString().replace('T', ' ').slice(0, 19);

const SwimlaneVisualization = ({
  config,
  data,
  width,
}: WidgetComponentProps<SwimlaneWidgetConfig, MessageListResult>) => {
  const theme = useTheme();
  const svgRef = useRef<SVGSVGElement | null>(null);
  const { handleClick, popover } = useSwimlaneClickPopover();
  const { openDetail, detailPanel, isOpen: isDetailOpen } = useSwimlaneDetailDrawer();
  const { laneFields, colorField, shapeField, shapeOverrides, labelField, tooltipFields, maxLanes, laneSort, laneSortField, laneSortAscending, correlationField } = config;
  const messages = data?.messages ?? [];
  const total = data?.total ?? 0;

  // Group by compound lane key
  const allLanes = useMemo(() => {
    const map = new Map<string, Lane['events']>();

    messages.forEach((bm) => {
      const { message } = bm;
      const key = laneFields.map((f) => String(message[f] ?? '(unknown)')).join(' / ');
      const ts = new Date(message.timestamp as string).getTime();
      const colorValue = colorField ? String(message[colorField] ?? '') : undefined;
      const shapeValue = shapeField ? String(message[shapeField] ?? '') : undefined;
      const labelValue = labelField ? String(message[labelField] ?? '') : undefined;
      const correlationValue = correlationField ? (String(message[correlationField] ?? '') || undefined) : undefined;

      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push({ ts, colorValue, shapeValue, labelValue, correlationValue, fields: message as Record<string, unknown>, backendMessage: bm });
    });

    return Array.from(map.entries())
      .map(([key, events]) => ({ key, events }))
      .sort((a, b) => b.events.length - a.events.length)
      .slice(0, maxLanes);
  }, [messages, laneFields, colorField, shapeField, labelField, maxLanes]);

  // Color mapping
  const colorValues = useMemo(() => {
    if (!colorField) return new Map<string, string>();
    const unique = [...new Set(allLanes.flatMap((l) => l.events.map((e) => e.colorValue ?? '')))];
    const colors = chroma.scale('Set2').colors(Math.max(unique.length, 2));

    return new Map(unique.map((v, i) => [v, colors[i % colors.length]]));
  }, [allLanes, colorField]);

  // Shape mapping
  const shapeIndex = useMemo((): Map<string, ShapeName> => {
    if (!shapeField) return new Map();
    const unique = [...new Set(allLanes.flatMap((l) => l.events.map((e) => e.shapeValue ?? '')))];

    return buildShapeIndex(unique, shapeOverrides);
  }, [allLanes, shapeField, shapeOverrides]);

  const dotFill = (colorValue: string | undefined): string => {
    if (!colorField || colorValue === undefined) return theme.colors.variant.info;

    return colorValues.get(colorValue) ?? theme.colors.variant.info;
  };

  // Sort lanes for display (top-N selection by event count stays on allLanes)
  const displayLanes = useMemo(() => {
    const sign = laneSortAscending ? 1 : -1;

    const compare = (a: Lane, b: Lane): number => {
      switch (laneSort) {
        case 'activity': {
          const aMax = a.events.length ? Math.max(...a.events.map((e) => e.ts)) : 0;
          const bMax = b.events.length ? Math.max(...b.events.map((e) => e.ts)) : 0;
          return aMax - bMax;
        }
        case 'firstOccurrence': {
          const aMin = a.events.length ? Math.min(...a.events.map((e) => e.ts)) : 0;
          const bMin = b.events.length ? Math.min(...b.events.map((e) => e.ts)) : 0;
          return aMin - bMin;
        }
        case 'alphabetical':
          return a.key.localeCompare(b.key);
        case 'fieldValue': {
          if (!laneSortField) return 0;
          const maxVal = (lane: Lane) => {
            const nums = lane.events.map((e) => Number(e.fields[laneSortField] ?? NaN)).filter(Number.isFinite);
            return nums.length ? Math.max(...nums) : -Infinity;
          };
          return maxVal(a) - maxVal(b);
        }
        case 'eventCount':
        default:
          return a.events.length - b.events.length;
      }
    };

    return [...allLanes].sort((a, b) => sign * compare(a, b));
  }, [allLanes, laneSort, laneSortField, laneSortAscending]);

  // Build flat render list — group headers + lane rows when multiple lane fields
  const isGrouped = laneFields.length > 1;

  const renderItems = useMemo((): RenderItem[] => {
    if (!isGrouped) {
      return displayLanes.map((lane, rowIndex) => ({
        type: 'lane' as const,
        lane,
        labelText: lane.key,
        labelField: laneFields[0],
        labelValue: lane.key,
        y: PADDING + rowIndex * LANE_HEIGHT,
        rowIndex,
      }));
    }

    const items: RenderItem[] = [];
    const seenGroups = new Set<string>();
    let y = PADDING;
    let rowIndex = 0;

    displayLanes.forEach((lane) => {
      const parts = lane.key.split(' / ');
      const primaryValue = parts[0];

      if (!seenGroups.has(primaryValue)) {
        seenGroups.add(primaryValue);
        items.push({ type: 'header', primaryValue, primaryField: laneFields[0], y });
        y += GROUP_HEADER_HEIGHT;
      }

      const subKey = parts.slice(1).join(' / ');
      items.push({
        type: 'lane',
        lane,
        labelText: subKey,
        labelField: laneFields[laneFields.length - 1],
        labelValue: parts[parts.length - 1],
        y,
        rowIndex,
      });
      y += LANE_HEIGHT;
      rowIndex += 1;
    });

    return items;
  }, [isGrouped, displayLanes, laneFields]);

  const totalPlotH = renderItems.reduce(
    (sum, item) => sum + (item.type === 'header' ? GROUP_HEADER_HEIGHT : LANE_HEIGHT),
    0,
  );

  // Data time range
  const { dataMinTs, dataMaxTs } = useMemo(() => {
    const allTs = allLanes.flatMap((l) => l.events.map((e) => e.ts));
    if (allTs.length === 0) return { dataMinTs: 0, dataMaxTs: 1 };

    return { dataMinTs: Math.min(...allTs), dataMaxTs: Math.max(...allTs) };
  }, [allLanes]);

  const svgWidth = isDetailOpen ? Math.max(0, Math.floor(width / 2)) : width;
  const plotW = Math.max(0, svgWidth - LABEL_WIDTH - PADDING);

  const { viewMin, viewMax, xOf, brushRect, isBrushing, isViewNarrowed, mouseHandlers } = useSwimlaneTimeNav({
    dataMinTs,
    dataMaxTs,
    plotW,
    labelWidth: LABEL_WIDTH,
    svgRef,
  });

  if (!laneFields.length) {
    return <EmptyState>Select at least one lane field in the widget settings.</EmptyState>;
  }

  if (allLanes.length === 0) {
    return <EmptyState>No data. Check your search query and time range.</EmptyState>;
  }

  const svgH = totalPlotH + AXIS_HEIGHT + PADDING;
  const plotH = totalPlotH;
  const truncated = total > messages.length;
  const lanesHidden = data?.total !== undefined && allLanes.length === maxLanes;

  // Build connector elements between correlated events
  const connectorElements: React.ReactElement[] = [];

  if (correlationField) {
    type ChainPoint = { cx: number; cy: number; ts: number; fill: string };
    const chains = new Map<string, ChainPoint[]>();

    renderItems.forEach((item) => {
      if (item.type !== 'lane') return;
      const cy = item.y + LANE_HEIGHT / 2;

      item.lane.events.forEach((ev) => {
        if (!ev.correlationValue) return;
        const cx = xOf(ev.ts);
        if (cx < LABEL_WIDTH - DOT_RADIUS || cx > svgWidth + DOT_RADIUS) return;
        if (!chains.has(ev.correlationValue)) chains.set(ev.correlationValue, []);
        chains.get(ev.correlationValue)!.push({ cx, cy, ts: ev.ts, fill: dotFill(ev.colorValue) });
      });
    });

    chains.forEach((points, corrVal) => {
      const sorted = [...points].sort((a, b) => a.ts - b.ts);
      if (sorted.length < 2) return;

      for (let i = 0; i < sorted.length - 1; i += 1) {
        const { cx: x1, cy: y1, fill } = sorted[i];
        const { cx: x2, cy: y2 } = sorted[i + 1];
        const sameRow = Math.abs(y2 - y1) < 1;
        const midY = (y1 + y2) / 2;
        const pathD = sameRow
          ? `M${x1},${y1} L${x2},${y2}`
          : `M${x1},${y1} C${x1},${midY} ${x2},${midY} ${x2},${y2}`;

        // Arrowhead chevron at destination dot edge, pointing in arrival direction
        const offset = DOT_RADIUS + 2;
        const arrowAngle = sameRow ? 0 : Math.sign(y2 - y1) * 90;
        const ahX = sameRow ? x2 - offset : x2;
        const ahY = sameRow ? y2 : y2 - Math.sign(y2 - y1) * offset;

        connectorElements.push(
          <g key={`conn-${corrVal}-${i}`}>
            <path d={pathD} stroke={fill} strokeWidth={1.5} fill="none" opacity={0.45} />
            <path
              d="M-5,-3 L0,0 L-5,3"
              transform={`translate(${ahX},${ahY}) rotate(${arrowAngle})`}
              stroke={fill}
              strokeWidth={1.5}
              fill="none"
              opacity={0.45}
            />
          </g>,
        );
      }
    });
  }

  return (
    <Wrapper>
      <SwimlaneColumn>
        {truncated && (
          <Banner>
            Showing first {messages.length} of {total} events — narrow your time range for more detail.
          </Banner>
        )}
        {lanesHidden && !truncated && (
          <Banner>
            Showing top {maxLanes} lanes by event count. Increase &quot;Max lanes&quot; in settings to see more.
          </Banner>
        )}
        {isViewNarrowed && (
          <ZoomBanner>
            Zoomed in — use the time range picker in the search bar to reset.
          </ZoomBanner>
        )}

        <svg ref={svgRef} width={svgWidth} height={svgH} style={{ flexShrink: 0, cursor: 'col-resize' }} {...mouseHandlers}>
          {/* Layer 1: backgrounds and labels */}
          {renderItems.map((item) => {
            if (item.type === 'header') {
              return (
                <g key={`header-${item.primaryValue}`}>
                  <rect x={0} y={item.y} width={svgWidth} height={GROUP_HEADER_HEIGHT}
                    fill={theme.colors.table.row.backgroundStriped} />
                  <rect x={0} y={item.y} width={3} height={GROUP_HEADER_HEIGHT}
                    fill={theme.colors.variant.info} />
                  <text
                    x={10}
                    y={item.y + GROUP_HEADER_HEIGHT / 2}
                    textAnchor="start"
                    dominantBaseline="middle"
                    fontSize="0.78em"
                    fontWeight="600"
                    fill={theme.colors.text.primary}
                    style={{ userSelect: 'none', cursor: 'pointer' }}
                    onClick={(e) => handleClick(e, item.primaryField, item.primaryValue)}>
                    {item.primaryValue.length > 24 ? `${item.primaryValue.slice(0, 22)}…` : item.primaryValue}
                  </text>
                </g>
              );
            }

            const { lane, labelText, labelField: itemLabelField, labelValue, y, rowIndex } = item;
            const isEven = rowIndex % 2 === 0;

            return (
              <g key={`bg-${lane.key}`}>
                <rect
                  x={0}
                  y={y}
                  width={svgWidth}
                  height={LANE_HEIGHT}
                  fill={isEven ? theme.colors.global.contentBackground : theme.colors.table.row.backgroundStriped}
                />
                <text
                  x={LABEL_WIDTH - 8}
                  y={y + LANE_HEIGHT / 2}
                  textAnchor="end"
                  dominantBaseline="middle"
                  fontSize="0.8em"
                  fill={theme.colors.text.secondary}
                  style={{ userSelect: 'none', cursor: 'pointer' }}
                  onClick={(e) => handleClick(e, itemLabelField, labelValue)}>
                  {labelText.length > 22 ? `${labelText.slice(0, 20)}…` : labelText}
                </text>
              </g>
            );
          })}

          {/* Layer 2: connectors between correlated events */}
          {connectorElements}

          {/* Layer 3: dots (rendered above connectors) */}
          {renderItems.map((item) => {
            if (item.type !== 'lane') return null;
            const { lane, y } = item;

            return (
              <g key={`dots-${lane.key}`}>
                {lane.events.map((ev, j) => {
                  const cx = xOf(ev.ts);
                  if (cx < LABEL_WIDTH - DOT_RADIUS || cx > svgWidth + DOT_RADIUS) return null;
                  const cy = y + LANE_HEIGHT / 2;
                  const fill = dotFill(ev.colorValue);
                  const shape = shapeField ? (shapeIndex.get(ev.shapeValue ?? '') ?? 'circle') : 'circle';

                  const laneLines = laneFields.map((f, fi) => `${f}: ${lane.key.split(' / ')[fi] ?? '(unknown)'}`);
                  const timeLine = `time: ${formatTs(ev.ts)}`;
                  const extraLines = tooltipFields.length
                    ? tooltipFields.filter((f) => ev.fields[f] != null).map((f) => `${f}: ${String(ev.fields[f])}`)
                    : (colorField && ev.colorValue ? [`${colorField}: ${ev.colorValue}`] : []);
                  const tooltipContent = [...laneLines, timeLine, ...extraLines].join('\n');

                  return (
                    // eslint-disable-next-line react/no-array-index-key
                    <Tooltip key={j} label={<span style={{ whiteSpace: 'pre' }}>{tooltipContent}</span>} withArrow position="top">
                      <g style={{ cursor: isBrushing ? 'col-resize' : 'pointer' }}>
                        {renderDot(shape, {
                          cx,
                          cy,
                          r: DOT_RADIUS,
                          fill,
                          opacity: 0.85,
                          onClick: () => { if (!isBrushing) openDetail(ev.backendMessage, lane.key); },
                        })}
                        {labelField && ev.labelValue && (
                          <text
                            x={cx}
                            y={cy - DOT_RADIUS - 2}
                            textAnchor="middle"
                            fontSize="0.6em"
                            fill={fill}
                            style={{ pointerEvents: 'none', userSelect: 'none' }}>
                            {ev.labelValue.slice(0, 8)}
                          </text>
                        )}
                      </g>
                    </Tooltip>
                  );
                })}
              </g>
            );
          })}

          {/* X-axis ticks from current view window */}
          {[0, 0.25, 0.5, 0.75, 1].map((fraction) => {
            const ts = viewMin + fraction * (viewMax - viewMin || 1);
            const cx = xOf(ts);
            const axisY = PADDING + totalPlotH;

            return (
              <g key={fraction}>
                <line x1={cx} y1={PADDING} x2={cx} y2={axisY} stroke={theme.colors.table.row.backgroundStriped} strokeWidth={1} />
                <text
                  x={cx}
                  y={axisY + 4}
                  textAnchor="middle"
                  dominantBaseline="hanging"
                  fontSize="0.7em"
                  fill={theme.colors.text.secondary}>
                  {formatTs(ts).slice(11)}
                </text>
              </g>
            );
          })}

          {brushRect && (
            <rect
              x={brushRect.x}
              y={PADDING}
              width={brushRect.width}
              height={plotH}
              fill={theme.colors.variant.info}
              opacity={0.15}
              pointerEvents="none"
            />
          )}
        </svg>
      </SwimlaneColumn>
      {detailPanel}
      {popover}
    </Wrapper>
  );
};

export default SwimlaneVisualization;
