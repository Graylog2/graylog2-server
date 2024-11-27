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
  NoTimeRangeOverride, AbsoluteTimeRange, RelativeTimeRange,
} from 'views/logic/queries/Query';
import {
  isTypeKeyword,
  isTypeRelativeWithStartOnly,
  isTypeRelativeWithEnd,
  isNoTimeRangeOverride,
} from 'views/typeGuards/timeRange';
import { readableRange } from 'views/logic/queries/TimeRangeToString';
import assertUnreachable from 'logic/assertUnreachable';

export const EMPTY_RANGE = '----/--/-- --:--:--.---';
export const EMPTY_OUTPUT = { from: EMPTY_RANGE, until: EMPTY_RANGE };

const TimeRangeWrapper = styled.div(({ theme }) => css`
  width: 100%;
  padding: 3px 13px;
  display: flex;
  justify-content: space-around;
  background-color: ${theme.colors.table.row.backgroundStriped};
  align-items: center;

  > span {
    flex: 1;
  }
`);

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

const TimeRange = ({ timerange }: { timerange: TimeRangeType | null | undefined }) => {
  if (isTypeKeyword(timerange)) {
    return <span>Keyword: <b>{timerange.keyword}</b></span>;
  }

  const { from, until } = range(timerange);

  return (
    <>
      <span data-testid="from">From: <b>{from}</b></span>
      <span data-testid="to">Until: <b>{until}</b></span>
    </>
  );
};

type Props = {
  timerange: TimeRangeType | NoTimeRangeOverride | null | undefined,
  toggleDropdownShow?: () => void,
};

const TimeRangeDisplay = ({ timerange, toggleDropdownShow }: Props) => (
  <TimeRangeWrapper aria-label="Search Time Range, Opens Time Range Selector On Click" role="button" onClick={toggleDropdownShow}>
    {isNoTimeRangeOverride(timerange)
      ? <span>No Override</span>
      : <TimeRange timerange={timerange} />}
  </TimeRangeWrapper>
);

export default TimeRangeDisplay;
