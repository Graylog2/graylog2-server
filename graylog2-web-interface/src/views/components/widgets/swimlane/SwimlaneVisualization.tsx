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
import chroma from 'chroma-js';

import type { WidgetComponentProps } from 'views/types';
import type { MessageListResult } from 'views/components/widgets/MessageList';
import type SwimlaneWidgetConfig from 'views/logic/widgets/SwimlaneWidgetConfig';
import Tooltip from 'components/common/Tooltip';
import useSwimlaneClickPopover from 'views/components/widgets/swimlane/useSwimlaneClickPopover';

const LANE_HEIGHT = 36;
const DOT_RADIUS = 5;
const LABEL_WIDTH = 160;
const AXIS_HEIGHT = 24;
const PADDING = 8;

const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
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

const EmptyState = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    color: ${theme.colors.text.secondary};
  `,
);

type Lane = {
  key: string;
  events: Array<{ ts: number; colorValue: string | undefined; fields: Record<string, unknown> }>;
};

const formatTs = (ts: number): string => new Date(ts).toISOString().replace('T', ' ').slice(0, 19);

const SwimlaneVisualization = ({
  config,
  data,
  width,
}: WidgetComponentProps<SwimlaneWidgetConfig, MessageListResult>) => {
  const theme = useTheme();
  const { handleClick, popover } = useSwimlaneClickPopover();
  const { laneField, colorField, maxLanes } = config;
  const messages = data?.messages ?? [];
  const total = data?.total ?? 0;

  // Group by laneField — preserve insertion order (messages are sorted ASC by timestamp)
  const allLanes = useMemo(() => {
    const map = new Map<string, Lane['events']>();

    messages.forEach(({ message }) => {
      const key = String(message[laneField] ?? '(unknown)');
      const ts = new Date(message.timestamp as string).getTime();
      const colorValue = colorField ? String(message[colorField] ?? '') : undefined;

      if (!map.has(key)) map.set(key, []);
      map.get(key)!.push({ ts, colorValue, fields: message as Record<string, unknown> });
    });

    // Sort lanes by event count descending, cap at maxLanes
    return Array.from(map.entries())
      .map(([key, events]) => ({ key, events }))
      .sort((a, b) => b.events.length - a.events.length)
      .slice(0, maxLanes);
  }, [messages, laneField, colorField, maxLanes]);

  // Color scale for the colorField values
  const colorValues = useMemo(() => {
    if (!colorField) return new Map<string, string>();
    const unique = [...new Set(allLanes.flatMap((l) => l.events.map((e) => e.colorValue ?? '')))];
    const colors = chroma.scale('Set2').colors(Math.max(unique.length, 2));

    return new Map(unique.map((v, i) => [v, colors[i % colors.length]]));
  }, [allLanes, colorField]);

  const dotColor = (colorValue: string | undefined): string => {
    if (!colorField || colorValue === undefined) return theme.colors.variant.info;

    return colorValues.get(colorValue) ?? theme.colors.variant.info;
  };

  // Time range from first/last event across all lanes
  const { minTs, maxTs } = useMemo(() => {
    const allTs = allLanes.flatMap((l) => l.events.map((e) => e.ts));

    if (allTs.length === 0) return { minTs: 0, maxTs: 1 };

    return { minTs: Math.min(...allTs), maxTs: Math.max(...allTs) };
  }, [allLanes]);

  if (!laneField) {
    return <EmptyState>Select a lane field in the widget settings.</EmptyState>;
  }

  if (allLanes.length === 0) {
    return <EmptyState>No data. Check your search query and time range.</EmptyState>;
  }

  const svgH = allLanes.length * LANE_HEIGHT + AXIS_HEIGHT + PADDING;
  const plotW = width - LABEL_WIDTH - PADDING;
  const tsRange = maxTs - minTs || 1;

  const xOf = (ts: number) => LABEL_WIDTH + ((ts - minTs) / tsRange) * plotW;

  const truncated = total > messages.length;
  const lanesHidden = data?.total !== undefined && allLanes.length === maxLanes;

  return (
    <>
    <Wrapper>
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

      <svg width={width} height={svgH} style={{ flexShrink: 0 }}>
        {/* Lane backgrounds and labels */}
        {allLanes.map((lane, i) => {
          const y = PADDING + i * LANE_HEIGHT;
          const isEven = i % 2 === 0;

          return (
            <g key={lane.key}>
              <rect
                x={0}
                y={y}
                width={width}
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
                onClick={(e) => handleClick(e, laneField, lane.key)}>
                {lane.key.length > 22 ? `${lane.key.slice(0, 20)}…` : lane.key}
              </text>

              {/* Dots */}
              {lane.events.map((ev, j) => {
                const cx = xOf(ev.ts);
                const cy = y + LANE_HEIGHT / 2;
                const label = [
                  `${laneField}: ${lane.key}`,
                  `time: ${formatTs(ev.ts)}`,
                  colorField && ev.colorValue ? `${colorField}: ${ev.colorValue}` : null,
                ]
                  .filter(Boolean)
                  .join('\n');

                const dotField = colorField && ev.colorValue ? colorField : laneField;
                const dotValue = colorField && ev.colorValue ? ev.colorValue : lane.key;

                return (
                  // eslint-disable-next-line react/no-array-index-key
                  <Tooltip key={j} label={<span style={{ whiteSpace: 'pre' }}>{label}</span>} withArrow position="top">
                    <circle
                      cx={cx}
                      cy={cy}
                      r={DOT_RADIUS}
                      fill={dotColor(ev.colorValue)}
                      opacity={0.85}
                      style={{ cursor: 'pointer' }}
                      onClick={(e) => handleClick(e, dotField, dotValue)}
                    />
                  </Tooltip>
                );
              })}
            </g>
          );
        })}

        {/* X-axis ticks */}
        {[0, 0.25, 0.5, 0.75, 1].map((fraction) => {
          const ts = minTs + fraction * tsRange;
          const x = xOf(ts);
          const axisY = PADDING + allLanes.length * LANE_HEIGHT;

          return (
            <g key={fraction}>
              <line x1={x} y1={PADDING} x2={x} y2={axisY} stroke={theme.colors.table.row.backgroundStriped} strokeWidth={1} />
              <text
                x={x}
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
      </svg>
    </Wrapper>
    {popover}
    </>
  );
};

export default SwimlaneVisualization;
