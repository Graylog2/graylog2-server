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

import type {
  TimeRange as TimeRangeType,
  NoTimeRangeOverride,
  AbsoluteTimeRange,
  RelativeTimeRange,
} from 'views/logic/queries/Query';
import { isTypeRelativeWithStartOnly, isTypeRelativeWithEnd, isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { readableRange } from 'views/logic/queries/TimeRangeToString';
import assertUnreachable from 'logic/assertUnreachable';

export const EMPTY_RANGE = '----/--/-- --:--:--.---';
export const EMPTY_OUTPUT = { from: EMPTY_RANGE, until: EMPTY_RANGE };

const TimeRangeWrapper = styled.div<{ $centerTimestamps: boolean }>(
  ({ theme, $centerTimestamps }) => css`
    width: 100%;
    background-color: ${theme.colors.table.row.backgroundStriped};
    align-items: center;

    ${$centerTimestamps
      ? css`
          padding: 3px 0;
          display: grid;
          grid-template-columns: 4fr 0.75fr 4fr;
        `
      : css`
          padding: 3px 13px;
          display: flex;
          justify-content: space-around;
        `}
  `,
);

const TimeRangeCell = styled.span<{
  $centerTimestamps: boolean;
  $gridColumn?: '1' | '3';
  $fullWidth?: boolean;
}>(
  ({ $centerTimestamps, $gridColumn, $fullWidth }) => css`
    ${$centerTimestamps
      ? css`
          min-width: 0;
          text-align: center;
          grid-column: ${$fullWidth ? '1 / -1' : $gridColumn};
        `
      : css`
          flex: 1;
        `}
  `,
);

const TimeRangeGap = styled.span<{ $centerTimestamps: boolean }>(({ $centerTimestamps }) =>
  $centerTimestamps
    ? css`
        grid-column: 2;
      `
    : css`
        display: none;
      `,
);

export const range = (timerange: AbsoluteTimeRange | RelativeTimeRange | null | undefined) => {
  let from = EMPTY_RANGE;
  let to = EMPTY_RANGE;

  if (!timerange?.type) {
    return EMPTY_OUTPUT;
  }

  const { type } = timerange;

  switch (type) {
    case 'relative':
      if (isTypeRelativeWithStartOnly(timerange)) {
        from = readableRange(timerange, 'range');
      }

      if (isTypeRelativeWithEnd(timerange)) {
        from = readableRange(timerange, 'from');
      }

      to = readableRange(timerange, 'to', 'Now');

      return {
        from,
        until: to,
      };

    case 'absolute':
      return { from: timerange.from, until: timerange.to };
    default:
      return assertUnreachable(type, 'Invalid time range type');
  }
};

const TimeRange = ({
  centerTimestamps,
  timerange,
}: {
  centerTimestamps: boolean;
  timerange: TimeRangeType | null | undefined;
}) => {
  if (timerange?.type === 'keyword') {
    return (
      <TimeRangeCell $centerTimestamps={centerTimestamps} $fullWidth={centerTimestamps}>
        Keyword: <b>{timerange.keyword}</b>
      </TimeRangeCell>
    );
  }

  const { from, until } = range(timerange);

  return (
    <>
      <TimeRangeCell data-testid="from" $centerTimestamps={centerTimestamps} $gridColumn="1">
        From: <b>{from}</b>
      </TimeRangeCell>
      <TimeRangeGap aria-hidden $centerTimestamps={centerTimestamps} />
      <TimeRangeCell data-testid="to" $centerTimestamps={centerTimestamps} $gridColumn="3">
        Until: <b>{until}</b>
      </TimeRangeCell>
    </>
  );
};

type Props = {
  timerange: TimeRangeType | NoTimeRangeOverride | null | undefined;
  toggleDropdownShow?: () => void;
  centerTimestamps?: boolean;
};

const TimeRangeDisplay = ({ timerange, toggleDropdownShow = undefined, centerTimestamps = false }: Props) => (
  <TimeRangeWrapper
    $centerTimestamps={centerTimestamps}
    aria-label="Search Time Range, Opens Time Range Selector On Click"
    role="button"
    onClick={toggleDropdownShow}>
    {isNoTimeRangeOverride(timerange) ? (
      <TimeRangeCell $centerTimestamps={centerTimestamps} $fullWidth={centerTimestamps}>
        No Override
      </TimeRangeCell>
    ) : (
      <TimeRange centerTimestamps={centerTimestamps} timerange={timerange} />
    )}
  </TimeRangeWrapper>
);

export default TimeRangeDisplay;
