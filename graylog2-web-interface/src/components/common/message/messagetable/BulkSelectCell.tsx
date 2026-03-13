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
import React from 'react';
import styled from 'styled-components';

import { BULK_SELECT_COLUMN_WIDTH } from 'components/common/EntityDataTable/Constants';

const StyledCell = styled.td`
  width: ${BULK_SELECT_COLUMN_WIDTH}px;
`;
// 'data-bulk-select-cell'
const BulkSelectCell = ({ children = null }: React.PropsWithChildren) => (
  <StyledCell onClick={(event) => event.stopPropagation()}>{children}</StyledCell>
);

export default BulkSelectCell;
