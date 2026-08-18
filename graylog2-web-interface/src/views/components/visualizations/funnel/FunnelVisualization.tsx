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
import styled, { css, useTheme } from 'styled-components';
import chroma from 'chroma-js';

import type { VisualizationComponentProps } from 'views/components/aggregationbuilder/AggregationBuilder';
import { makeVisualization } from 'views/components/aggregationbuilder/AggregationBuilder';
import type FunnelVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/FunnelVisualizationConfig';
import {
  DEFAULT_FUNNEL_START_COLOR,
  DEFAULT_FUNNEL_END_COLOR,
} from 'views/logic/aggregationbuilder/visualizations/FunnelVisualizationConfig';
import Tooltip from 'components/common/Tooltip';

import { computeFunnelGeometry, LABEL_AREA, MIN_LABEL_PX } from './funnelGeometry';
import { formatCount, subPartColors } from './buildStages';
import useFunnelStages from './useFunnelStages';
import useFunnelOnClickPopover from './useFunnelOnClickPopover';

export const FUNNEL_VISUALIZATION_TYPE = 'funnel' as const;

const Container = styled.div`
  height: 100%;
  width: 100%;
  overflow: hidden;
`;

const EmptyStateWrapper = styled.div(
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

type PartRect = { y: number; h: number; color: string; label: string; value: number };

const FunnelVisualization = makeVisualization(({ config, data, width, height }: VisualizationComponentProps) => {
  const theme = useTheme();
  const { stages, rowFields, colFields } = useFunnelStages(config, data);
  const { handleRectClick, popover } = useFunnelOnClickPopover();

  if (!stages || width === 0 || height === 0) {
    return <EmptyStateWrapper>No data to display. Add a grouping field and a metric.</EmptyStateWrapper>;
  }

  // ── Colors ──────────────────────────────────────────────────────────────
  const vizConfig = config.visualizationConfig as FunnelVisualizationConfig | undefined;
  const startColor = vizConfig?.startColor ?? DEFAULT_FUNNEL_START_COLOR;
  const endColor = vizConfig?.endColor ?? DEFAULT_FUNNEL_END_COLOR;
  const stageColors = chroma.scale([startColor, endColor]).mode('lab').colors(stages.length);

  // ── Geometry ────────────────────────────────────────────────────────────
  const { nW, stageH, top, nl, transitionPath } = computeFunnelGeometry(stages, width, height);

  const layoutParts = (stageIdx: number): PartRect[] => {
    const stage = stages[stageIdx];
    const totalH = stageH(stageIdx);
    const colors = subPartColors(stageColors[stageIdx], stage.parts.length);
    let y = top(stageIdx);

    return stage.parts.map((part, partIdx) => {
      const h = (part.value / stage.value) * totalH;
      const rect: PartRect = { y, h, color: colors[partIdx], label: part.label, value: part.value };
      y += h;

      return rect;
    });
  };

  const rowField = rowFields[0] ?? '';
  const colField = colFields[0] ?? '';

  return (
    <>
      <Container>
        <svg width={width} height={height}>
          {/* ── Stage boxes ─────────────────────────────────────────────── */}
          {stages.map((stage, i) => {
            if (stage.parts.length === 0) {
              return (
                <rect
                  // eslint-disable-next-line react/no-array-index-key
                  key={i}
                  x={nl(i)} y={top(i)} width={nW} height={stageH(i)}
                  fill={stageColors[i]}
                  style={{ cursor: 'pointer' }}
                  onClick={(e) => handleRectClick(e, rowField, stage.label)}
                />
              );
            }

            return (
              // eslint-disable-next-line react/no-array-index-key
              <g key={i}>
                {layoutParts(i).map((pr, partIdx) => (
                  // eslint-disable-next-line react/no-array-index-key
                  <g key={partIdx}>
                    <Tooltip label={`${pr.label}: ${formatCount(pr.value)}`} withArrow position="top" disabled={pr.h >= MIN_LABEL_PX}>
                      <rect
                        x={nl(i)} y={pr.y} width={nW} height={pr.h}
                        fill={pr.color}
                        style={{ cursor: 'pointer' }}
                        onClick={(e) => handleRectClick(e, colField, pr.label)}
                      />
                    </Tooltip>

                    {pr.h >= MIN_LABEL_PX && (
                      <text x={nl(i) + nW / 2} y={pr.y + pr.h / 2} textAnchor="middle" dominantBaseline="middle" fontSize="1em" fontWeight="500" fill="white" style={{ pointerEvents: 'none' }}>
                        {`${pr.label}  ${formatCount(pr.value)}`}
                      </text>
                    )}
                  </g>
                ))}
              </g>
            );
          })}

          {/* ── Bezier transitions ──────────────────────────────────────── */}
          {stages.slice(0, -1).map((_, i) => (
            // eslint-disable-next-line react/no-array-index-key
            <path key={i} d={transitionPath(i)} fill={stageColors[i + 1]} />
          ))}


          {/* ── Stage labels (name + total count) ───────────────────────── */}
          {stages.map((stage, i) => (
            // eslint-disable-next-line react/no-array-index-key
            <g key={i} style={{ cursor: 'pointer' }} onClick={(e) => handleRectClick(e, rowField, stage.label)}>
              <rect x={nl(i)} y={0} width={nW} height={LABEL_AREA} fill="transparent" />
              <text x={nl(i) + nW / 2} y={20} textAnchor="middle" fontSize="0.75em" fill={theme.colors.text.secondary} style={{ pointerEvents: 'none' }}>
                {stage.label}
              </text>
              <text x={nl(i) + nW / 2} y={52} textAnchor="middle" fontSize="1.5em" fontWeight="bold" fill={theme.colors.text.primary} style={{ pointerEvents: 'none' }}>
                {formatCount(stage.value)}
              </text>
            </g>
          ))}
        </svg>
      </Container>

      {/* Rendered outside Container so it isn't clipped by overflow:hidden */}
      {popover}
    </>
  );
}, FUNNEL_VISUALIZATION_TYPE);

FunnelVisualization.displayName = 'FunnelVisualization';

export default FunnelVisualization;
