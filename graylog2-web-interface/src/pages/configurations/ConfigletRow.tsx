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

import { Row } from 'components/graylog';

const ConfigletRow = styled(Row)`
  .deflist {
    margin-top: 10px;
  }

  .deflist dt {
    float: left;
    clear: left;
  }

  .deflist dd {
    margin-left: 160px;
  }

  .top-margin {
    margin-top: 10px;
  }

  hr.separator {
    margin-top: 10px;
    margin-bottom: 5px;
  }
`;

export default ConfigletRow;
