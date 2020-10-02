// @flow strict
import * as React from 'react';
import styled, { css } from 'styled-components';
import moment from 'moment';

type Props = {
  timerange: {
    type?: string,
    range?: number,
    to?: string,
    from?: string,
    keyword?: string,
  },
};

const TimeRange = styled.p(({ theme }) => css`
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

const dateOutput = ({ type, ...restRange }) => {
  let from = '----/--/-- --:--:--.---';

  switch (type) {
    case 'relative':
      from = restRange.range === 0 ? 'All Time' : moment().subtract(restRange.range * 1000).fromNow();

      return { from, until: 'Now' };
    case 'absolute':
      return { from: restRange.from, until: restRange.to };
    case 'keyword':
      return { from: restRange.keyword, until: 'Now' };
    default:
      return { from, until: from };
  }
};

const TimeRangeDisplay = ({ timerange }: Props) => {
  if (!timerange?.type) {
    return (
      <TimeRange>
        <span><code>No Override</code></span>
      </TimeRange>
    );
  }

  const { from, until } = dateOutput(timerange);

  return (
    <TimeRange>
      <span><strong>From</strong>: <code>{from}</code></span>
      <span><strong>Until</strong>: <code>{until}</code></span>
    </TimeRange>
  );
};

export default TimeRangeDisplay;
