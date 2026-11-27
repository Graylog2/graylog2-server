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

import { Alert } from 'components/bootstrap';
import { Link } from 'components/common';
import RoutePaths from 'routing/Routes';

const ErrorList = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.xs};
  margin: ${({ theme }) => theme.spacings.sm} 0;
  padding-left: ${({ theme }) => theme.spacings.lg};
  list-style-type: disc;
  overflow-wrap: anywhere;
`;

type Props = {
  nodeId: string | null;
  errors: Record<string, string> | undefined;
};

// Every Data Node applies the same settings, so the same error usually comes from all of them
const groupByMessage = (errors: Record<string, string>) =>
  Object.entries(errors).reduce<Map<string, number>>(
    (messages, [, message]) => messages.set(message, (messages.get(message) ?? 0) + 1),
    new Map(),
  );

const LiveSettingsErrorAlert = ({ nodeId, errors }: Props) => {
  if (!errors || Object.keys(errors).length === 0) return null;

  const messages = [...groupByMessage(errors).entries()];
  const showCounts = nodeId === null && Object.keys(errors).length > 1;

  return (
    <Alert bsStyle="danger" title="OpenSearch did not accept some saved settings">
      Settings shared by all Data Nodes take effect without a restart, but OpenSearch refused to apply the following:
      <ErrorList>
        {messages.map(([message, count]) => (
          <li key={message}>
            {message}
            {showCounts && ` (reported by ${count} Data Node${count === 1 ? '' : 's'})`}
          </li>
        ))}
      </ErrorList>
      Correct or remove the value
      {nodeId === null ? (
        ' below'
      ) : (
        <>
          {' '}
          in <Link to={RoutePaths.SYSTEM.CLUSTER.DATANODE_CONFIGURATION}>Data Node Configuration</Link>
        </>
      )}
      . The Data Nodes keep trying, and this message disappears once they succeed.
    </Alert>
  );
};

export default LiveSettingsErrorAlert;
