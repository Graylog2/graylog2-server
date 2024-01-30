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
import type { DataNode } from 'preflight/types';

const StatusLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

type Props = {
  dataNode: DataNode,
};

const DataNodeStatusCell = ({ dataNode }: Props) => {
  const datanodeDisabled = dataNode.data_node_status !== 'AVAILABLE';

  return (
    <StatusLabel bsStyle={datanodeDisabled ? 'warning' : 'success'}
                 title={dataNode.data_node_status}
                 aria-label={dataNode.data_node_status}>
      {dataNode.data_node_status}
    </StatusLabel>
  );
};

export default DataNodeStatusCell;
