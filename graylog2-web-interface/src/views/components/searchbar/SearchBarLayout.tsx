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
import styled, { css } from 'styled-components';

import { FlatContentRow } from 'components/common';

export const SEARCH_BAR_GAP = '10px';

export const SearchBarContainer = styled(FlatContentRow)`
  display: flex;
  flex-direction: column;
  gap: ${SEARCH_BAR_GAP};
`;

export const SearchQueryRow = styled.div(({ theme }) => css`
  display: flex;
  gap: ${SEARCH_BAR_GAP};
  align-items: flex-start;

  @media (max-width: ${theme.breakpoints.max.sm}) {
    flex-direction: column;
  
    > div {
      width: 100%;
    }
  }
`);

export const SearchButtonAndQuery = styled.div`
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: ${SEARCH_BAR_GAP};
`;

export const SearchInputAndValidationContainer = styled.div`
  display: flex;
  flex: 1;
`;

export const TimeRangeRow = styled.div(({ theme }) => css`
  display: flex;
  gap: ${SEARCH_BAR_GAP};
  align-items: flex-start;

  @media (max-width: ${theme.breakpoints.max.md}) {
    flex-direction: column;

    > div {
      width: 100%;
    }
  }
`);
