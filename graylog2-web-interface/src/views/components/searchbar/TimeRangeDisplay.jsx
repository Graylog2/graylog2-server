// @flow strict
import * as React from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import moment from 'moment';

import { type ThemeInterface } from 'theme';
import { type TimeRange } from 'views/logic/queries/Query';

type Props = {|
  timerange: ?TimeRange,
|};

const TimeRangeWrapper: StyledComponent<{}, ThemeInterface, HTMLParagraphElement> = styled.p(({ theme }) => css`
  width: 100%;
  padding: 3px 9px;
  margin: 0 12px;
  display: flex;
  justify-content: space-around;
  background-color: ${theme.colors.variant.lightest.primary};
  align-items: center;
  border-radius: 4px;
  
  > span {
    flex: 1;
  }
  
  code {
    color: ${theme.colors.variant.dark.primary};
    background: transparent;
    font-size: ${theme.fonts.size.body};
  }
`);

const dateOutput = (timerange: TimeRange) => {
  let from = '----/--/-- --:--:--.---';

  switch (timerange.type) {
    case 'relative':
      from = !timerange.range ? 'All Time' : moment()
        .subtract(timerange.range * 1000)
        .fromNow();

      return {
        from,
        until: 'Now',
      };

    case 'absolute':
      return { from: timerange.from, until: timerange.to };
    case 'keyword':
      return { from: timerange.keyword, until: 'TODO' };
    default:
      return { from, until: from };
  }
};

const TimeRangeDisplay = ({ timerange }: Props) => {
  if (!timerange?.type) {
    return (
      <TimeRangeWrapper>
        <span><code>No Override</code></span>
      </TimeRangeWrapper>
    );
  }

  const { from, until } = dateOutput(timerange);

  return (
    <TimeRangeWrapper>
      <span><strong>From</strong>: <code>{from}</code></span>
      <span><strong>Until</strong>: <code>{until}</code></span>
    </TimeRangeWrapper>
  );
};

export default TimeRangeDisplay;
