// @flow strict
import * as React from 'react';
import { useEffect, useRef, useState } from 'react';
import styled, { css, type StyledComponent } from 'styled-components';
import moment from 'moment';

import { type ThemeInterface } from 'theme';
import { type TimeRange } from 'views/logic/queries/Query';
import StoreProvider from 'injection/StoreProvider';

type Props = {|
  timerange: ?TimeRange,
|};

export const EMPTY_RANGE = '----/--/-- --:--:--.---';
export const EMPTY_OUTPUT = { from: EMPTY_RANGE, until: EMPTY_RANGE };

const ToolsStore = StoreProvider.getStore('Tools');

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
  let from = EMPTY_RANGE;

  if (!timerange) {
    return EMPTY_OUTPUT;
  }

  if (timerange?.range >= 0) {
    from = !timerange.range ? 'All Time' : moment()
      .subtract(timerange.range * 1000)
      .fromNow();

    return {
      from,
      until: 'Now',
    };
  }

  if (timerange?.from && timerange?.to) {
    return { from: timerange.from, until: timerange.to };
  }

  throw new Error('Invalid Timerange Type');
};

const TimeRangeDisplay = ({ timerange }: Props) => {
  const [{ from, until }, setTimeOutput] = useState(EMPTY_OUTPUT);
  const dateTested = useRef(false);

  useEffect(() => {
    if (timerange?.keyword && !timerange.from) {
      if (!dateTested.current) {
        ToolsStore.testNaturalDate(timerange.keyword)
          .then((response) => {
            dateTested.current = true;

            setTimeOutput({
              from: response.from,
              until: response.to,
            });
          }, () => {
            setTimeOutput(EMPTY_OUTPUT);
          });
      }
    } else {
      setTimeOutput(dateOutput(timerange));
    }
  }, [dateTested, timerange]);

  return (
    <TimeRangeWrapper>
      {(!timerange || !Object.keys(timerange).length)
        ? <span><code>No Override</code></span>
        : (
          <>
            <span><strong>From</strong>: <code>{from}</code></span>
            <span><strong>Until</strong>: <code>{until}</code></span>
          </>
        )}
    </TimeRangeWrapper>
  );
};

export default TimeRangeDisplay;
