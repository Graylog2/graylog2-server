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

import { RowContentStyles } from 'components/graylog/Row';

/*
 * Component with the same styling like the bootstrap `Row` but with a smaller padding.
 */
const FlatContentRow = styled.div`
  ${RowContentStyles}
  padding: 9px;
  margin-left: -15px;
  margin-right: -15px;

  .row {
    margin-left: -9px;
    margin-right: -9px;
  }

  div[class*="col-"] {
    padding-right: 9px;
    padding-left: 9px;
  }
`;

export default FlatContentRow;
