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
import { useState, useEffect } from 'react';

import useTriggerMigrationState from './useTriggerMigrationState';

import type { MigrationActions } from '../Types';

export type MigrationStatus = 'NOT_STARTED'|'STARTING'|'RUNNING'|'ERROR'|'FINISHED';

export type RemoteReindexIndex = {
  took: string,
  batches: number,
  error_msg: string,
  created: string,
  name: string,
  status: MigrationStatus,
}

export type RemoteReindexMigration = {
  indices: RemoteReindexIndex[],
  id: string,
  error: string,
  status: MigrationStatus,
  progress: number,
}

export type RemoteReindexRequest = {
  hostname: string,
  password: string,
  indices: string[],
  synchronous: boolean,
  user: string,
}

const useRemoteReindexMigrationStatus = () : {
  nextSteps: MigrationActions[],
  migrationStatus: RemoteReindexMigration,
} => {
  const [nextSteps, setNextSteps] = useState<MigrationActions[]>(['RETRY_MIGRATE_EXISTING_DATA']);
  const [migrationStatus, setMigrationStatus] = useState<RemoteReindexMigration>(null);
  const { onTriggerNextState } = useTriggerMigrationState();

  useEffect(() => {
    const fetchCurrentMigrationStatus = async () => {
      if (migrationStatus?.progress !== 100) {
        onTriggerNextState({ step: 'REQUEST_MIGRATION_STATUS' }).then((data) => {
          const _migrationStatus = data?.response as RemoteReindexMigration;

          if (_migrationStatus) {
            setMigrationStatus(_migrationStatus);

            if (_migrationStatus?.progress === 100) {
              setNextSteps(['RETRY_MIGRATE_EXISTING_DATA', 'SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER']);
            }
          }
        });
      }
    };

    const interval = setInterval(() => {
      fetchCurrentMigrationStatus();
    }, 3000);

    return () => clearInterval(interval);
  }, [onTriggerNextState, migrationStatus]);

  return ({
    nextSteps,
    migrationStatus,
  });
};

export default useRemoteReindexMigrationStatus;
