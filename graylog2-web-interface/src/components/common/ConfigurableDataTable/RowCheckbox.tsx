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

import { Checkbox } from 'components/bootstrap';

const RowCheckbox = styled(Checkbox)`
  &.checkbox {
    margin: 0;
  
    label {
      display: flex;
      align-items: center;
      padding: 0;
      
      input {
        width: 14px;
        height: 14px;
        cursor: pointer;
        margin: 0;
        position: relative;
      }
    }
  }
`;

export default RowCheckbox;
