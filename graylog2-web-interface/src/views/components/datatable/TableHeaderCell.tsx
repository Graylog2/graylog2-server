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

import { VISUALIZATION_TABLE_HEADER_HEIGHT } from 'views/Constants';

const TableHeaderCell = styled.th<{ $isNumeric?: boolean, $borderedHeader?: boolean }>(({ $isNumeric, $borderedHeader, theme }) => css`
  && {
    border: ${$borderedHeader ? `1px solid ${theme.colors.table.backgroundAlt}` : '0'};
    background-color: ${theme.colors.gray[90]};
    height: ${VISUALIZATION_TABLE_HEADER_HEIGHT}px;
    padding: 0 5px;
    vertical-align: middle;
    white-space: nowrap;
    font-weight: normal;
    font-size: ${theme.fonts.size.small};
    color: ${theme.utils.readableColor(theme.colors.gray[90])};
    ${$isNumeric ? 'text-align: right' : ''}
  }
`);

export default TableHeaderCell;
