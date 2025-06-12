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

import moment from 'moment/moment';
import type { Moment } from 'moment/moment';

import { DATE_TIME_FORMATS } from 'util/DateTime';
import type { ChartDefinition } from 'views/components/visualizations/ChartData';
import { PLOTLY_DEFAULT_GAP, BAR_ADJUSTMENT_COEFFICIENT } from 'views/components/visualizations/Constants';

type RangeProps = { minX: string; maxX: string };

export const getChartWidthFromChartDataArray = (chartDataArray: Array<ChartDefinition>) => {
  if (!Array.isArray(chartDataArray) || chartDataArray.length === 0) {
    throw new Error('Input must be a non-empty array of ChartData.');
  }

  let globalMinDelta = Infinity;

  // eslint-disable-next-line no-restricted-syntax
  for (const item of chartDataArray) {
    const xArray = item.x || [];

    const sorted = xArray.map((dateStr) => moment.parseZone(dateStr)).sort((a, b) => a.diff(b));

    // eslint-disable-next-line no-plusplus
    for (let i = 1; i < sorted.length; i++) {
      const delta = sorted[i].diff(sorted[i - 1]);
      if (delta < globalMinDelta) {
        globalMinDelta = delta;
      }
    }
  }

  return globalMinDelta * (1 - PLOTLY_DEFAULT_GAP);
};

const adjustBorder = (sortedDates: Array<Moment>, candidate: Moment, minDelta: number) => {
  const right = sortedDates.find((d) => d.isAfter(candidate));
  const left = [...sortedDates].reverse().find((d) => d.isBefore(candidate));

  const leftDiff = left?.isValid?.() ? candidate.diff(left) : minDelta;
  const rightDiff = right?.isValid?.() ? right.diff(candidate) : minDelta;

  if (leftDiff < minDelta) return left.clone().add(minDelta, 'milliseconds');
  if (rightDiff < minDelta) return right.clone().subtract(minDelta, 'milliseconds');

  return candidate;
};

export const findDateRangeFromChartData = (
  allDates: Array<Moment>,
  normalizedFrom: string,
  normalizedTo: string,
  minDelta: number,
): RangeProps => {
  const fromMoment = moment.parseZone(normalizedFrom);
  const toMoment = moment.parseZone(normalizedTo);

  const minX = adjustBorder(allDates, fromMoment, minDelta);
  const maxX = adjustBorder(allDates, toMoment, minDelta);

  return {
    minX: minX.format(DATE_TIME_FORMATS.internal),
    maxX: maxX.format(DATE_TIME_FORMATS.internal),
  };
};

const getAdjustedPlotTimelineRange = (
  chartDataArray: Array<ChartDefinition>,
  normalizedFrom: string,
  normalizedTo: string,
): RangeProps => {
  if (!chartDataArray?.length) return { minX: normalizedFrom, maxX: normalizedTo };

  const minDelta = getChartWidthFromChartDataArray(chartDataArray) * BAR_ADJUSTMENT_COEFFICIENT;
  const allDates: Array<Moment> = chartDataArray
    .flatMap((item) => item.x || [])
    .map((dateStr) => moment.parseZone(dateStr))
    .sort((a, b) => a.diff(b));

  return findDateRangeFromChartData(allDates, normalizedFrom, normalizedTo, minDelta);
};

export default getAdjustedPlotTimelineRange;
