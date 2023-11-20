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
import styled, { css } from 'styled-components';
import { useCallback } from 'react';

import { Icon } from 'components/common';
import { Label } from 'components/bootstrap';
import type { DataNode } from 'preflight/types';

const StatusLabel = styled(Label)(({ $clickable }: any) => css`
  cursor: ${$clickable ? 'pointer' : 'default'};
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`);

const Spacer = styled.div`
  border-left: 1px solid currentColor;
  height: 1em;
`;

const _title = (disabled: boolean, disabledChange: boolean, description: string) => {
  if (disabledChange) {
    return description;
  }

  return disabled ? 'Start dataNode' : 'Pause dataNode';
};

type Props = {
  dataNode: DataNode,
};

const DataNodeStatusCell = ({ dataNode }: Props) => {
  const disableChange = dataNode.is_leader || dataNode.is_master;
  const description = dataNode.status;
  const datanodeDisabled = false;
  const title = _title(datanodeDisabled, disableChange, description);

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
                 title={title}
                 aria-label={title}
                 role="button"
                 $clickable={!disableChange as any}>
      {datanodeDisabled ? 'Paused' : 'Running'}
      {!disableChange && (
        <>
          <Spacer />
          <Icon name={datanodeDisabled ? 'play' : 'pause'} />
        </>
      )}
    </StatusLabel>
  );
};

export default DataNodeStatusCell;
