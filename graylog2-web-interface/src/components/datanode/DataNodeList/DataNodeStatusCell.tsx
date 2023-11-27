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
import { useCallback } from 'react';

import { Label } from 'components/bootstrap';
import type { DataNode } from 'preflight/types';

const StatusLabel = styled(Label)`
  cursor: 'pointer';
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

type Props = {
  dataNode: DataNode,
};

const DataNodeStatusCell = ({ dataNode }: Props) => {
  const disableChange = dataNode.is_leader || dataNode.is_master;
  const datanodeDisabled = dataNode.status !== 'CONNECTED';

  const toggleStreamStatus = useCallback(async () => {
    if (datanodeDisabled) {
      // enable
    }

    // eslint-disable-next-line no-alert
    if (!datanodeDisabled && window.confirm(`Do you really want to pause datanode '${dataNode.hostname}'?`)) {
      // disable
    }
  }, [dataNode.hostname, datanodeDisabled]);

  return (
    <StatusLabel bsStyle={datanodeDisabled ? 'warning' : 'success'}
                 onClick={disableChange ? undefined : toggleStreamStatus}
                 title={dataNode.status}
                 aria-label={dataNode.status}
                 role="button">
      {dataNode.status}
    </StatusLabel>
  );
};

export default DataNodeStatusCell;
