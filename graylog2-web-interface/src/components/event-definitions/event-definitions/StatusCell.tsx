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
import * as React from 'react';
import styled from 'styled-components';

import { Label } from 'components/bootstrap';

const StatusLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

type Props ={
  status: boolean
}

const StatusCell = ({ status } : Props) => {
  const isSuccess = status;

  return (
    <StatusLabel bsStyle={isSuccess ? 'success' : 'warning'}>
      {isSuccess ? 'enabled' : 'disabled'}
    </StatusLabel>
  );
};

export default StatusCell;
