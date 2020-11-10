// @flow strict
import * as React from 'react';
import { useEffect, useState } from 'react';
import styled, { css, type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';
import { Icon } from 'components/common';
import { type TimeRange } from 'views/logic/queries/Query';
import DateTime from 'logic/datetimes/DateTime';

import { EMPTY_OUTPUT, dateOutput } from '../TimeRangeDisplay';

type Props = {|
  timerange: TimeRange,
|};

const PreviewWrapper: StyledComponent<{}, void, HTMLDivElement> = styled.div`
  display: flex;
  align-items: center;
  justify-content: flex-end;
  float: right;
  transform: translateY(-3px);
`;

const FromWrapper: StyledComponent<{}, void, HTMLSpanElement> = styled.span`
  text-align: right;
`;

const UntilWrapper: StyledComponent<{}, void, HTMLSpanElement> = styled.span`
  text-align: left;
`;

const Title: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  color: ${theme.colors.variant.dark.info};
  display: block;
  font-style: italic;
`);

const Date: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.body};
  color: ${theme.colors.variant.primary};
  display: block;
  font-weight: bold;
`);

const MiddleIcon: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  color: ${theme.colors.variant.default};
  padding: 0 15px;
`);

const dateTimeString = (value) => (DateTime.isValidDateString(value) ? value : 'Incorrect Date Format');

const TimeRangeLivePreview = ({ timerange }: Props) => {
  const [{ from, until }, setTimeOutput] = useState(EMPTY_OUTPUT);

  useEffect(() => {
    setTimeOutput(dateOutput(timerange));
  }, [timerange]);

  return (
    <PreviewWrapper>
      <FromWrapper>
        <Title>From</Title>
        <Date title={`Date Formatted as [${DateTime.Formats.TIMESTAMP}]`}>{dateTimeString(from)}</Date>
      </FromWrapper>

      <MiddleIcon>
        <Icon name="arrow-right" />
      </MiddleIcon>

      <UntilWrapper>
        <Title>Until</Title>
        <Date title={`Date Formatted as [${DateTime.Formats.TIMESTAMP}]`}>{dateTimeString(until)}</Date>
      </UntilWrapper>
    </PreviewWrapper>
  );
};

export default TimeRangeLivePreview;
