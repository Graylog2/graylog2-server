import * as React from 'react';
import { useEffect, useState } from 'react';
import styled, { css, StyledComponent } from 'styled-components';
import moment from 'moment';
import PropTypes from 'prop-types';

import type { ThemeInterface } from 'theme';
import type { TimeRange } from 'views/logic/queries/Query';
import { Icon } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

import { EMPTY_OUTPUT, EMPTY_RANGE } from '../TimeRangeDisplay';

type Props = {
  timerange?: TimeRange,
};

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

const dateOutput = (timerange: TimeRange) => {
  let range = EMPTY_RANGE;

  if (!timerange) {
    return EMPTY_OUTPUT;
  }

  if ('range' in timerange) {
    range = !timerange.range ? 'All Time' : moment()
      .subtract(timerange.range * 1000)
      .fromNow();

    return {
      from: range,
      until: 'Now',
    };
  }

  return {
    from: timerange.from || range,
    until: timerange.to || range,
  };
};

const TimeRangeLivePreview = ({ timerange }: Props) => {
  const [{ from, until }, setTimeOutput] = useState(EMPTY_OUTPUT);

  useEffect(() => {
    const output = dateOutput(timerange);

    setTimeOutput(output);
  }, [timerange]);

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
    from: PropTypes.string,
    to: PropTypes.string,
  }),
};

TimeRangeLivePreview.defaultProps = {
  timerange: undefined,
};

export default TimeRangeLivePreview;
