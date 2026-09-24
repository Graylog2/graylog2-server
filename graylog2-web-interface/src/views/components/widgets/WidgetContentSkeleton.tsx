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
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import { Skeleton } from 'components/common';

const BAR_HEIGHTS = [34, 48, 42, 60, 54, 70, 50, 64, 40, 56, 74, 58, 46, 66, 80, 62];
const HEATMAP_ROWS = 4;
const HEATMAP_COLUMNS = 8;
const WAVE_STEP_MS = 60;

const skeletonColor = (theme: DefaultTheme) => theme.utils.opacify(theme.colors.text.primary, 0.18);

const Container = styled.div(
  ({ theme }) => css`
    display: flex;
    flex: 1;
    min-height: 0;
    min-width: 0;
    height: 100%;

    /* The default skeleton color is too bright in dark mode and its body-colored backdrop differs from the card. */
    .mantine-Skeleton-root::before {
      background-color: transparent;
    }

    .mantine-Skeleton-root::after {
      background-color: ${skeletonColor(theme)};
    }
  `,
);
const NumberLayout = styled.div(
  ({ theme }) => css`
    display: flex;
    flex: 1;
    flex-direction: column;
    justify-content: flex-end;
    gap: ${theme.spacings.xs};
    padding-bottom: ${theme.spacings.xxs};
  `,
);

const ValueLine = styled(Skeleton)`
  max-height: 40px;
`;

const Bars = styled.div(
  ({ theme }) => css`
    display: flex;
    flex: 1;
    align-items: flex-end;
    justify-content: space-between;
    gap: ${theme.spacings.xs};
    padding: 0 ${theme.spacings.xxs};
    border-bottom: 1px solid ${skeletonColor(theme)};
  `,
);

const waveDelay = (index: number) => css`
  &::after {
    animation-delay: -${index * WAVE_STEP_MS}ms;
  }
`;

const Bar = styled(Skeleton)<{ $index: number }>(
  ({ $index }) => css`
    && {
      flex: 1 1 0;
      min-width: 0;
      max-width: 28px;
      border-radius: 3px 3px 0 0;
    }

    ${waveDelay($index)}
  `,
);

const HeatmapGrid = styled.div(
  ({ theme }) => css`
    display: grid;
    flex: 1;
    grid-template-columns: repeat(${HEATMAP_COLUMNS}, 1fr);
    grid-template-rows: repeat(${HEATMAP_ROWS}, 1fr);
    gap: ${theme.spacings.xxs};
  `,
);

const HeatmapCell = styled(Skeleton)<{ $index: number; $intensity: number }>(
  ({ $index, $intensity }) => css`
    && {
      border-radius: 2px;
      opacity: ${$intensity};
    }

    ${waveDelay($index)}
  `,
);

const heatmapIntensity = (row: number, column: number) => 0.35 + ((row * 3 + column * 5) % 7) / 10;

const NumberSkeleton = () => (
  <NumberLayout>
    <ValueLine height="55%" width="45%" />
    <Skeleton height={10} width="25%" />
  </NumberLayout>
);

const ChartSkeleton = () => (
  <Bars>
    {BAR_HEIGHTS.map((height, index) => (
      // eslint-disable-next-line react/no-array-index-key
      <Bar key={index} $index={index} height={`${height}%`} />
    ))}
  </Bars>
);

const HeatmapSkeleton = () => (
  <HeatmapGrid>
    {Array.from({ length: HEATMAP_ROWS * HEATMAP_COLUMNS }, (_, index) => {
      const row = Math.floor(index / HEATMAP_COLUMNS);
      const column = index % HEATMAP_COLUMNS;

      return (
        <HeatmapCell
          key={index}
          $index={row + column}
          $intensity={heatmapIntensity(row, column)}
          height="100%"
        />
      );
    })}
  </HeatmapGrid>
);

const skeletonForVisualization = (visualization: string | undefined) => {
  switch (visualization) {
    case 'numeric':
      return <NumberSkeleton />;
    case 'heatmap':
      return <HeatmapSkeleton />;
    default:
      return <ChartSkeleton />;
  }
};

type Props = {
  visualization?: string;
};

/**
 * Placeholder for a widget's content while it is loading, roughly shaped like the given visualization.
 */
const WidgetContentSkeleton = ({ visualization = undefined }: Props) => (
  <Container aria-busy="true" aria-label="Loading widget content" role="status">
    {skeletonForVisualization(visualization)}
  </Container>
);

export default WidgetContentSkeleton;
