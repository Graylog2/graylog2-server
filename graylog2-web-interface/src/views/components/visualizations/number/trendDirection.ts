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
import type { DefaultTheme } from 'styled-components';

import type { TrendPreference } from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

export type TrendDirection = 'good' | 'bad' | 'neutral';

export const trendBackground = (theme: DefaultTheme, trend: TrendDirection = 'neutral') =>
  ({
    good: theme.colors.variant.success,
    bad: theme.colors.variant.danger,
    neutral: theme.colors.global.contentBackground,
  })[trend];

export const diff = (current: number | undefined, previous: number | undefined): [number, number] => {
  if (typeof current === 'number' && typeof previous === 'number') {
    const difference = current - previous;
    const differencePercent = difference / previous;

    return [difference, differencePercent];
  }

  return [NaN, NaN];
};

const trendDirection = (
  current: number | undefined,
  previous: number | undefined | null,
  trendPreference: TrendPreference,
): TrendDirection => {
  const [delta] = diff(current, previous ?? undefined);

  if (!Number.isFinite(delta) || delta === 0) {
    return 'neutral';
  }

  switch (trendPreference) {
    case 'LOWER':
      return delta > 0 ? 'bad' : 'good';
    case 'HIGHER':
      return delta > 0 ? 'good' : 'bad';
    case 'NEUTRAL':
    default:
      return 'neutral';
  }
};

export default trendDirection;
