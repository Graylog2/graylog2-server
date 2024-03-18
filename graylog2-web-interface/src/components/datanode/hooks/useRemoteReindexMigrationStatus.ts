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

import type { MigrationActions, MigrationState, OnTriggerStepFunction, StepArgs } from '../Types';
import { MIGRATION_STATE } from '../Constants';

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

export const RemoteReindexFinishedStatusActions: MigrationActions[] = ['RETRY_MIGRATE_EXISTING_DATA', 'SHOW_ASK_TO_SHUTDOWN_OLD_CLUSTER'];

const useRemoteReindexMigrationStatus = (
  currentStep: MigrationState,
  onTriggerStep: OnTriggerStepFunction,
  refetchInterval: number = 3000,
) : {
  nextSteps: MigrationActions[],
  migrationStatus: RemoteReindexMigration,
  handleTriggerStep: OnTriggerStepFunction,
} => {
  const [nextSteps, setNextSteps] = useState<MigrationActions[]>(['RETRY_MIGRATE_EXISTING_DATA']);
  const [migrationStatus, setMigrationStatus] = useState<RemoteReindexMigration>(undefined);

  useEffect(() => {
    const fetchCurrentMigrationStatus = async () => {
      if (currentStep?.state === MIGRATION_STATE.REMOTE_REINDEX_RUNNING.key) {
        if (
          migrationStatus?.progress === 100
          && migrationStatus?.status === 'FINISHED'
        ) {
          setNextSteps(currentStep?.next_steps.filter((action) => RemoteReindexFinishedStatusActions.includes(action)));
        } else {
          onTriggerStep('REQUEST_MIGRATION_STATUS').then((data) => {
            const _migrationStatus = data?.response as RemoteReindexMigration;

            if (_migrationStatus) {
              setMigrationStatus(_migrationStatus);
            }
          });
        }
      }
    };

    const interval = setInterval(() => {
      fetchCurrentMigrationStatus();
    }, refetchInterval);

    return () => clearInterval(interval);
  }, [onTriggerStep, migrationStatus, currentStep?.state, currentStep?.next_steps, refetchInterval]);

  const handleTriggerStep = (step: MigrationActions, args?: StepArgs) => onTriggerStep(step, args).then((data) => {
    setMigrationStatus(undefined);

    return data;
  });

  return ({
    nextSteps,
    migrationStatus,
    handleTriggerStep,
  });
};

export default useRemoteReindexMigrationStatus;
