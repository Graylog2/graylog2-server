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
import styled from 'styled-components';

const DefinitionList = styled.dl`
  width: 100%;
  margin: 0;

  dt {
    color: ${({ theme }) => theme.colors.text.secondary};
    font-weight: normal;
    padding: ${({ theme }) => theme.spacings.xxs} ${({ theme }) => theme.spacings.xxs} 0;
    overflow-wrap: break-word;
    word-break: break-word;
  }

  dd {
    margin: 0;
    padding: 0 ${({ theme }) => theme.spacings.xxs} ${({ theme }) => theme.spacings.xxs};
    border-bottom: 1px solid ${({ theme }) => theme.colors.table.row.divider};
    overflow-wrap: break-word;
    word-break: break-word;
  }

  dt:nth-of-type(odd),
  dd:nth-of-type(odd) {
    background-color: ${({ theme }) => theme.colors.table.row.backgroundStriped};
  }
`;

export default DefinitionList;
