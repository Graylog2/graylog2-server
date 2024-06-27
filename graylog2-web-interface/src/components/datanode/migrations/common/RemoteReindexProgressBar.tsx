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
import React, { useState } from 'react';
import styled from 'styled-components';

import { ProgressBar } from 'components/common';
import { Button } from 'components/bootstrap';

import type { MigrationStatus, RemoteReindexMigration } from '../../hooks/useRemoteReindexMigrationStatus';

const MainProgressBar = styled(ProgressBar)`
  margin-bottom: 0;
`;

const TaskProgressBar = styled(ProgressBar)(({ theme }) => `
  margin-top: ${theme.spacings.sm};
  margin-bottom: ${theme.spacings.sm};
`);

const displayStatus = (status: MigrationStatus): string => {
  switch (status) {
    case 'NOT_STARTED':
      return 'LOADING...';
    case 'STARTING':
      return 'STARTING...';
    case 'RUNNING':
      return 'RUNNING...';
    default:
      return status || '';
  }
};

type Props = {
  migrationStatus: RemoteReindexMigration;
}

const RemoteReindexProgressBar = ({ migrationStatus }: Props) => {
  const [showTasks, setShowTasks] = useState<boolean>(false);

  const progress = migrationStatus?.progress || 0;
  const status = displayStatus(migrationStatus?.status);
  const tasks_progress = migrationStatus?.tasks_progress || {};

  return (
    <>
      <MainProgressBar bars={[{
        animated: true,
        value: progress,
        bsStyle: 'info',
        label: `${status} ${progress}%`,
      }]} />
      {Object.keys(tasks_progress).length > 0 && (
        <>
          <Button bsStyle="link" bsSize="xs" onClick={() => setShowTasks(!showTasks)}>
            {showTasks ? 'Hide tasks' : 'Show tasks'}
          </Button>
          {showTasks && Object.keys(tasks_progress).map((task) => (
            <TaskProgressBar bars={[{
              animated: false,
              value: tasks_progress[task] || 0,
              bsStyle: 'info',
              label: `${task} ${tasks_progress[task] || 0}%`,
            }]} />
          ))}
        </>
      )}
    </>
  );
};

export default RemoteReindexProgressBar;
