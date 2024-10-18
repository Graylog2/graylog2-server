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

const TableHeaderCell = styled.th<{ $isNumeric?: boolean, $borderedHeader?: boolean }>(({ $isNumeric, $borderedHeader, theme }) => css`
  && {
    background-color: ${theme.colors.table.head.background};
    min-width: 50px;
    border: ${$borderedHeader ? `1px solid ${theme.colors.table.row.divider}` : '0'};
    padding: 0 5px;
    vertical-align: middle;
    white-space: nowrap;
    font-weight: normal;
    font-size: ${theme.fonts.size.small};
    ${$isNumeric ? 'text-align: right' : ''}
  }
`);

export default TableHeaderCell;
