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

const TableHead = styled.thead(({ theme }) => css`
  background-color: ${theme.colors.global.contentBackground};
  height: ${VISUALIZATION_TABLE_HEADER_HEIGHT}px;
  color: ${theme.colors.global.textDefault};
  position: sticky;
  top: 0;
  z-index: 1;
`);

export default TableHead;
