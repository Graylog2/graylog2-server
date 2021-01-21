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
import { useEffect, useState } from 'react';
import styled, { css } from 'styled-components';
import moment from 'moment';
import PropTypes from 'prop-types';
import { useFormikContext } from 'formik';

import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import { Icon } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';
import { SearchBarFormValues } from 'views/Constants';

import { EMPTY_OUTPUT, EMPTY_RANGE } from '../TimeRangeDisplay';

type Props = {
  timerange?: TimeRange | NoTimeRangeOverride,
};

const PreviewWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  float: right;
  transform: translateY(-3px);
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
  padding: 0 15px;
`);

const readableRange = (timerange: TimeRange, fieldName: 'range' | 'from' | 'to', placeholder = 'All Time') => {
  return !timerange[fieldName] ? placeholder : moment()
    .subtract(timerange[fieldName] * 1000)
    .fromNow();
};

const dateOutput = (timerange: TimeRange | NoTimeRangeOverride) => {
  let from = EMPTY_RANGE;
  let to = EMPTY_RANGE;

  if (!timerange) {
    return EMPTY_OUTPUT;
  }

  if ('type' in timerange && timerange.type === 'relative') {
    if ('range' in timerange) {
      from = readableRange(timerange, 'range');
    }

    if ('from' in timerange) {
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
  const { isValid } = useFormikContext<SearchBarFormValues>();
  const [{ from, until }, setTimeOutput] = useState(EMPTY_OUTPUT);

  useEffect(() => {
    let output = EMPTY_OUTPUT;

    if (isValid) {
      output = dateOutput(timerange);
    }

    setTimeOutput(output);
  }, [isValid, timerange]);

  return (
    <PreviewWrapper>
      <FromWrapper>
        <Title>From</Title>
        <Date title={`Dates Formatted as [${DateTime.Formats.TIMESTAMP}]`}>{from}</Date>
      </FromWrapper>

      <MiddleIcon>
        <Icon name="arrow-right" />
      </MiddleIcon>

      <UntilWrapper>
        <Title>Until</Title>
        <Date title={`Dates Formatted as [${DateTime.Formats.TIMESTAMP}]`}>{until}</Date>
      </UntilWrapper>
    </PreviewWrapper>
  );
};

TimeRangeLivePreview.propTypes = {
  timerange: PropTypes.shape({
    range: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    from: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    to: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }),
};

TimeRangeLivePreview.defaultProps = {
  timerange: undefined,
};

export default TimeRangeLivePreview;
