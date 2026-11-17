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

import { CELL_PADDING_HORIZONTAL } from 'components/common/EntityDataTable/Constants';

// About ten one-line items. Longer lists scroll, so the row doesn't grow with the list.
const EXPANDED_MAX_HEIGHT = 240; // px

const CellListScrollArea = styled.div`
  min-width: 0;
  max-height: ${EXPANDED_MAX_HEIGHT}px;
  overflow: hidden auto;

  /* Extends into the cell padding, so the scrollbar sits at the cell border. */
  margin-right: -${CELL_PADDING_HORIZONTAL}px;
  padding-right: ${CELL_PADDING_HORIZONTAL}px;
`;

export default CellListScrollArea;
