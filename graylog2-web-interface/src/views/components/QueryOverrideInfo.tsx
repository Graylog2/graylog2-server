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

import type { ElasticsearchQueryString } from 'views/logic/queries/Query';
import { Button } from 'components/graylog';

import QueryInput from './searchbar/QueryInput';

const Wrapper = styled.div`
  width: 30%;
  display: flex;
  align-items: center;
  &::before {
    box-shadow: 17px 0 16px -16px rgb(0 0 0 / 40%) inset;
  }
`;

const TimeRangeInfo = styled.div(({ theme }) => `
  margin-left: 10px;
  border: 1px dashed ${theme.colors.input.border};
  display: flex;
  align-items: center;
  width: 100%;
  border-radius: 4px;
  padding: 0 5px;
  min-height: 34px;
  border-left: 0;

  .query {
    flex: 1;
  }
`);

const ResetButton = styled(Button)`
  margin-left: 5px;
`;

type Props = {
  query: ElasticsearchQueryString,
  onReset: () => void
};

const TimeRangeOverrideInfo = ({ query, onReset }: Props) => {
  return (
    <Wrapper>
      <TimeRangeInfo>
        Filter:
        <QueryInput onChange={() => Promise.resolve('')} onExecute={() => {}} disabled value={query.query_string} />
        <ResetButton bsSize="xs" bsStyle="primary" data-testid="reset-filter" onClick={onReset}>Reset</ResetButton>
      </TimeRangeInfo>
    </Wrapper>
  );
};

export default TimeRangeOverrideInfo;
