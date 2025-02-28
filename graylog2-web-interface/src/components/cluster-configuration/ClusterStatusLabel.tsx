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

import type { GraylogNode } from './useClusterNodes';

const StatusLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

type Props = {
  node: GraylogNode,
};

const ClusterStatusLabel = ({ node }: Props) => {
  const lifecycleStatus = node.lifecycle.toUpperCase()
  const loadBalancersStatus = `Load Balancers ${node.lb_status.toUpperCase()}`
  const messageProcessingStatus = `Message Processing ${node.is_processing ? 'ENABLED' : 'DISABLED'}`

  return (
    <>
      <StatusLabel bsStyle={lifecycleStatus === 'RUNNING' ? 'success' : 'warning'}
                  title={lifecycleStatus}
                  aria-label={lifecycleStatus}>
        {lifecycleStatus}
      </StatusLabel>&nbsp;
      <StatusLabel bsStyle={node.lb_status === 'alive' ? 'success' : 'warning'}
                  title={loadBalancersStatus}
                  aria-label={loadBalancersStatus}>
      {loadBalancersStatus}
      </StatusLabel>&nbsp;
      <StatusLabel bsStyle={node.is_processing ? 'success' : 'warning'}
                  title={messageProcessingStatus}
                  aria-label={messageProcessingStatus}>
      {messageProcessingStatus}
      </StatusLabel>
    </>
  );
};

export default ClusterStatusLabel;
