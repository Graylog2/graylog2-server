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

import { DATE_TIME_FORMATS } from 'util/DateTime';

type RangeProps = { minX: string; maxX: string };
export const adjustRangeWithStep = ({ minX, maxX }: RangeProps, itemsLength: number): RangeProps => {
  const minMoment = moment(minX);
  const maxMoment = moment(maxX);

  const durationMs = maxMoment.diff(minMoment);
  const stepDurationMs = durationMs / itemsLength;

  const delta = 1 / 2;
  const adjustedMinX = minMoment.clone().subtract(stepDurationMs * delta, 'milliseconds');
  const adjustedMaxX = maxMoment.clone().add(stepDurationMs * delta, 'milliseconds');

  return {
    minX: adjustedMinX.format(DATE_TIME_FORMATS.internal),
    maxX: adjustedMaxX.format(DATE_TIME_FORMATS.internal),
  };
};
export const findDateRange = (
  chartDataArray: Array<{ x: Array<string> }>,
  normalizedFrom: string,
  normalizedTo: string,
): RangeProps => {
  if (!Array.isArray(chartDataArray) || chartDataArray.length === 0) {
    throw new Error('Input must be a non-empty array of ChartData.');
  }

  const fromMoment = moment(normalizedFrom);
  const toMoment = moment(normalizedTo);

  const allDates = chartDataArray
    .flatMap((d) => d.x || [])
    .map((dateStr) => moment(dateStr))
    .sort((a, b) => a.diff(b)); // Sort ascending

  const afterFrom = allDates.find((d) => d.isAfter(fromMoment));
  const beforeTo = [...allDates].reverse().find((d) => d.isBefore(toMoment));

  if (!afterFrom || !beforeTo) {
    throw new Error('Could not find valid minX or maxX within provided range.');
  }

  return {
    minX: afterFrom.format(DATE_TIME_FORMATS.internal),
    maxX: beforeTo.format(DATE_TIME_FORMATS.internal),
  };
};

const getAdjustedTimeRange = (
  chartDataArray: Array<{ x: Array<string> }>,
  normalizedFrom: string,
  normalizedTo: string,
  itemsLength: number,
): RangeProps => adjustRangeWithStep(findDateRange(chartDataArray, normalizedFrom, normalizedTo), itemsLength);

export default getAdjustedTimeRange;
