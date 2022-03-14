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
import styled, { css } from 'styled-components';

import type { ElasticsearchQueryString } from 'views/logic/queries/Query';
import { Button } from 'components/bootstrap';

import BasicQueryInput from './searchbar/queryinput/AsyncBasicQueryInput';

const Wrapper = styled.div`
  width: 25%;
  min-width: 300px;
  max-width: 500px;
  display: flex;
  align-items: center;

  &::before {
    box-shadow: 17px 0 16px -16px rgb(0 0 0 / 40%) inset;
  }
`;

const QueryInfo = styled.div(({ theme }) => css`
  margin-left: 10px;
  border: 1px dashed ${theme.colors.input.border};
  display: flex;
  align-items: center;
  width: 100%;
  border-top-right-radius: 4px;
  border-bottom-right-radius: 4px;
  padding: 0 5px;
  min-height: 34px;
  border-left: 0;

  .query {
    flex: 1;
  }
`);

const StyledBasicQueryInput = styled(BasicQueryInput)`
  &&.ace-queryinput {
    border: none;
  }
`;

const ResetButton = styled(Button)`
  margin-left: 5px;
`;

type Props = {
  value: ElasticsearchQueryString,
  onReset: () => void
};

const WidgetQueryOverride = ({ value, onReset }: Props) => (
  <Wrapper>
    <QueryInfo>
      <StyledBasicQueryInput disabled
                             value={value.query_string}
                             height={34}
                             wrapEnabled={false}
                             maxLines={1} />
      <ResetButton bsSize="xs" bsStyle="primary" onClick={onReset} data-testid="reset-global-query">
        Reset Global Filter
      </ResetButton>
    </QueryInfo>
  </Wrapper>
);

export default WidgetQueryOverride;
