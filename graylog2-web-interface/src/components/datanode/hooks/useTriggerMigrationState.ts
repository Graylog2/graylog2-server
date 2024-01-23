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
import UserNotification from 'util/UserNotification';
import { MIGRATION_STATE_QUERY_KEY } from 'components/datanode/hooks/useMigrationState';

const useTriggerMigrationState = () => {
  const queryClient = useQueryClient();
  const { mutateAsync: onTriggerNextState, isLoading: isLoadingNextMigrationState } = useMutation(Migration.migrate, {
    onSuccess: () => {
      UserNotification.success('Bundle updated successfully');
      queryClient.invalidateQueries(MIGRATION_STATE_QUERY_KEY);
    },
    onError: (error: Error) => UserNotification.error(error.message),
  });

  return {
    onTriggerNextState,
    isLoadingNextMigrationState,
  };
};

export default useTriggerMigrationState;
