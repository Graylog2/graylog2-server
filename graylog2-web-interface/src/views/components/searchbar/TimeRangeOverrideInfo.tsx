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
import { TimeRange } from 'src/views/logic/queries/Query';
import styled from 'styled-components';

import { Button } from 'components/graylog';

import TimeRangeButton from './TimeRangeButton';
import TimeRangeDisplay from './TimeRangeDisplay';

const Wrapper = styled.div`
  display: flex;
  align-items: center;
`;

const TimeRangeInfo = styled.div(({ theme }) => `
  margin-left: 10px;
  border: 1px dashed ${theme.colors.input.border};
  display: flex;
  align-items: center;
  width: 100%;
  border-radius: 4px;
  padding: 3px 5px;
  min-height: 34px;
`);

const ResetButton = styled(Button)`
  margin-left: 5px;
`;

type Props = {
  onReset: () => void,
  value: TimeRange,
};

const TimeRangeOverrideInfo = ({ value, onReset }: Props) => {
  return (
    <Wrapper>
      <TimeRangeButton disabled />
      <TimeRangeInfo>
        <TimeRangeDisplay timerange={value} />
        <ResetButton bsSize="xs" bsStyle="primary" onClick={onReset} data-testid="reset-global-time-range">
          Reset Global Override
        </ResetButton>
      </TimeRangeInfo>
    </Wrapper>
  );
};

export default TimeRangeOverrideInfo;
