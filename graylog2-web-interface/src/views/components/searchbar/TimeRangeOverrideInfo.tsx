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
import styled from 'styled-components';

import { TimeRange } from 'views/logic/queries/Query';
import { Button } from 'components/graylog';
import timerangeToString from 'views/logic/queries/TimeRangeToString';

import TimeRangeButton from './TimeRangeButton';

const Wrapper = styled.div`
  display: flex;
  align-items: center;
`;

const TimeRangeInfo = styled.div(({ theme }) => `
  margin-left: 10px;
  border: 1px dashed ${theme.colors.input.border};
  width: 100%;
  border-radius: 4px;
  padding: 0 5px;
  min-height: 34px;
`);

const TimeRangeString = styled.div(({ theme }) => `
  display: inline-block;
  margin-left: 0;
  margin-top: 5px;
  padding: 0 3px;
  border-radius: 4px;
  background-color: ${theme.colors.variant.lightest.primary};
  color: ${theme.colors.variant.darker.primary};
  font-size: ${theme.fonts.size.body};
  font-family: ${theme.fonts.family.monospace};
`);

const ResetButton = styled(Button)`
  margin-top: 5px;
  margin-bottom: 5px;
  margin-left: 5px;
  display: inline-block;
  float: right;
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
        <TimeRangeString>{timerangeToString(value)}</TimeRangeString>
        <ResetButton bsSize="xs" bsStyle="primary" onClick={onReset} data-testid="reset-global-time-range">
          Reset Global Override
        </ResetButton>
      </TimeRangeInfo>
    </Wrapper>
  );
};

export default TimeRangeOverrideInfo;
