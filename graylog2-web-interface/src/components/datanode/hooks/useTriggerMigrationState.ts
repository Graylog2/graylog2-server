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
import { useMutation, useQueryClient } from '@tanstack/react-query';

import { Migration } from '@graylog/server-api';

import { MIGRATION_STATE_QUERY_KEY } from 'components/datanode/hooks/useMigrationState';
import UserNotification from 'util/UserNotification';
import type { MigrationState, MigrationStepRequest } from 'components/datanode/Types';

const useTriggerMigrationState = (): {
  onTriggerNextState: (step: MigrationStepRequest) => Promise<MigrationState>,
  isLoadingNextMigrationState: boolean,
  isError: boolean,
  error: Error,
} => {
  const queryClient = useQueryClient();
  const { mutateAsync: onTriggerNextState, isLoading: isLoadingNextMigrationState, error, isError } = useMutation(Migration.trigger, {
    onSuccess: () => {
      queryClient.invalidateQueries(MIGRATION_STATE_QUERY_KEY);
    },
    onError: (err: Error) => UserNotification.error(err.message),
  });

  return {
    onTriggerNextState: onTriggerNextState as (step: MigrationStepRequest) => Promise<MigrationState>,
    isLoadingNextMigrationState,
    isError,
    error,
  };
};

export default useTriggerMigrationState;
