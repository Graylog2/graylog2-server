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
import { useContext, useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import PropTypes from 'prop-types';
import { useFormikContext } from 'formik';

import { readableRange } from 'views/logic/queries/TimeRangeToString';
import { isTypeRelative, isTypeRelativeWithEnd, isTypeRelativeWithStartOnly } from 'views/typeGuards/timeRange';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { Icon, IfPermitted } from 'components/common';
import { DATE_TIME_FORMATS } from 'util/DateTime';
import TimeRangeAddToQuickListButton from 'views/components/searchbar/time-range-picker/TimeRangeAddToQuickListButton';
import type { TimeRangeDropDownFormValues } from 'views/components/searchbar/time-range-picker/TimeRangePicker';
import TimeRangeInputSettingsContext from 'views/components/contexts/TimeRangeInputSettingsContext';

import { EMPTY_OUTPUT, EMPTY_RANGE } from '../time-range-filter/TimeRangeDisplay';

type Props = {
  timerange?: TimeRange | NoTimeRangeOverride,
};

const PreviewWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  float: right;
  transform: translateY(-3px);
  gap: 5px;
`;

const FromWrapper = styled.span`
  text-align: right;
`;

const UntilWrapper = styled.span`
  text-align: left;
`;

const Title = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  color: ${theme.colors.variant.darker.info};
  display: block;
  font-style: italic;
`);

const Date = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.body};
  color: ${theme.colors.variant.dark.primary};
  display: block;
  font-weight: bold;
`);

const MiddleIcon = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  color: ${theme.colors.variant.default};
  padding: 0 5px;
`);

export const dateOutput = (timerange: TimeRange | NoTimeRangeOverride) => {
  let from = EMPTY_RANGE;
  let to = EMPTY_RANGE;

  if (!timerange) {
    return EMPTY_OUTPUT;
  }

  if (isTypeRelative(timerange)) {
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
  }

  return {
    from: 'from' in timerange ? timerange.from : from,
    until: 'to' in timerange ? timerange.to : to,
  };
};

const TimeRangeLivePreview = ({ timerange }: Props) => {
  const { isValid, errors } = useFormikContext<TimeRangeDropDownFormValues>();
  const [{ from, until }, setTimeOutput] = useState(EMPTY_OUTPUT);
  const { showAddToQuickListButton } = useContext(TimeRangeInputSettingsContext);

  useEffect(() => {
    let output = EMPTY_OUTPUT;

    if (isValid) {
      output = dateOutput(timerange);
    }

    setTimeOutput(output);
  }, [isValid, timerange]);

  const isTimerangeValid = !errors.nextTimeRange;

  return (
    <PreviewWrapper data-testid="time-range-live-preview">
      <FromWrapper>
        <Title>From</Title>
        <Date title={`Dates Formatted as [${DATE_TIME_FORMATS.complete}]`}>{from}</Date>
      </FromWrapper>

      <MiddleIcon>
        <Icon name="arrow-right" />
      </MiddleIcon>

      <UntilWrapper>
        <Title>Until</Title>
        <Date title={`Dates Formatted as [${DATE_TIME_FORMATS.complete}]`}>{until}</Date>
      </UntilWrapper>
      {showAddToQuickListButton && !!(timerange as TimeRange).type && (
        <IfPermitted permissions="clusterconfigentry:edit">
          <TimeRangeAddToQuickListButton timerange={timerange as TimeRange} isTimerangeValid={isTimerangeValid} />
        </IfPermitted>
      )}
    </PreviewWrapper>
  );
};

TimeRangeLivePreview.propTypes = {
  timerange: PropTypes.object,
};

TimeRangeLivePreview.defaultProps = {
  timerange: undefined,
};

export default TimeRangeLivePreview;
