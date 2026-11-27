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

import type { ConfigurationParameter } from './types';
import { categoryId } from './configurationFields';
import { restartTargetOf } from './configurationRestart';

const RestartSettingsList = styled.ul`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacings.xs};
  margin-top: ${({ theme }) => theme.spacings.sm};
  padding-left: ${({ theme }) => theme.spacings.lg};
  list-style-type: disc;
`;

const Paragraph = styled.p`
  margin: ${({ theme }) => theme.spacings.sm} 0 0;
`;

type Props = {
  nodeId: string | null;
  basePath: string;
  restartParameters: Array<ConfigurationParameter>;
  hasUnsavedRestartChanges: boolean;
  pendingSharedRestart: boolean;
  canRestart: boolean;
};

const DataNodeRestartRequiredAlert = ({
  nodeId,
  basePath,
  restartParameters,
  hasUnsavedRestartChanges,
  pendingSharedRestart,
  canRestart,
}: Props) => {
  const restartTarget = restartTargetOf(nodeId);

  return (
    <Alert
      bsStyle="warning"
      title={hasUnsavedRestartChanges ? 'Restart required after saving' : 'Saved changes require a restart'}>
      {pendingSharedRestart ? (
        <>
          Settings shared by all Data Nodes have been saved, but this Data Node does not use them yet. Restart this Data
          Node for them to take effect. See{' '}
          <Link to={RoutePaths.SYSTEM.CLUSTER.DATANODE_CONFIGURATION}>Data Node Configuration</Link> for these settings.
        </>
      ) : (
        <>
          {hasUnsavedRestartChanges
            ? `Changes to the following settings require restarting ${restartTarget} to take effect:`
            : `The following settings have been saved. Restart ${restartTarget} for them to take effect:`}
          <RestartSettingsList>
            {restartParameters.map(({ name, label, category }) => (
              <li key={name}>
                <Link to={`${basePath}/${categoryId(category)}#${encodeURIComponent(name)}`}>{label}</Link>
              </li>
            ))}
          </RestartSettingsList>
        </>
      )}
      {!canRestart && (
        <Paragraph>
          You are not allowed to restart Data Nodes. Ask someone who is allowed to restart {restartTarget}.
        </Paragraph>
      )}
    </Alert>
  );
};

export default DataNodeRestartRequiredAlert;
