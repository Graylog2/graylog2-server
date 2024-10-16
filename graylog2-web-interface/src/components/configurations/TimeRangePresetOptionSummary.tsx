import React from 'react';
import styled from 'styled-components';

import assertUnreachable from 'logic/assertUnreachable';
import type { TimeRangePreset } from 'components/configurations/TimeRangePresetForm';
import type {
  KeywordTimeRange,
  TimeRange,
} from 'views/logic/queries/Query';
import { range } from 'views/components/searchbar/time-range-filter/TimeRangeDisplay';

type Props = { options : Array<TimeRangePreset>};

const StyledDL = styled.dl`
  && {
    > span {
      display: flex;
      gap: 5px;
    }
    
    dt {
      white-space: nowrap;
      flex-basis: 175px;
    }

    dd {
      margin: 0;
      flex: 1;
    }
  }
`;

export const getTimeRangeValueSummary = (timerange: TimeRange) => {
  switch (timerange.type) {
    case 'relative':
      return `${range(timerange).from} - ${range(timerange).until}`;
    case 'absolute':
      return `${range(timerange).from} - ${range(timerange).until}`;
    case 'keyword':
      return (timerange as KeywordTimeRange).keyword;
    default:
      return assertUnreachable(timerange, 'Timerange type doesn\'t not exist');
  }
};

const TimeRangePresetOptionSummary = ({ options }: Props) => (
  <StyledDL className="deflist">
    {options.map(({ timerange, id, description }) => (
      <span key={`timerange-options-summary-${id}`}>
        <dt>{getTimeRangeValueSummary(timerange)}</dt>
        <dd>{description}</dd>
      </span>
    ))}
  </StyledDL>
);

export default TimeRangePresetOptionSummary;
