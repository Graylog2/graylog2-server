// @ts-nocheck
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
import { useQuery } from '@tanstack/react-query';

import { Migration } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { MigrationState } from 'components/datanode/Types';
import { onError } from 'util/conditional/onError';

export const MIGRATION_STATE_QUERY_KEY = ['migration-state'];

const useMigrationState = (refetchInterval : number | false = false) : {
  currentStep: MigrationState,
  isLoading: boolean,
} => {
  const { data, isLoading } = useQuery<MigrationState, Error>(
    MIGRATION_STATE_QUERY_KEY,
    () => onError(Migration.status(), (error: Error) => UserNotification.error(error.message)),
    {
      retry: 2,
      refetchInterval,
    },
  );

  return {
    currentStep: data,
    isLoading,
  };
};

export default useMigrationState;
