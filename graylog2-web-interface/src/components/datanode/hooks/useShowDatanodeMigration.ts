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
import { useQuery } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import useCurrentUser from 'hooks/useCurrentUser';

import useMigrationState from './useMigrationState';

const fetchShowDatanodeMigration = async () => fetch('GET', qualifyUrl('/datanode/configured'));

const useShowDatanodeMigration = (): {
  isDatanodeConfiguredAndUsed: boolean;
  showDatanodeMigration: boolean;
} => {
  const { permissions } = useCurrentUser();
  const canStartDataNode = React.useMemo(
    () => permissions.includes('datanode:start') || permissions.includes('*'),
    [permissions],
  );

  const { data: isDatanodeConfiguredAndUsed } = useQuery(['show_datanode_migration'], fetchShowDatanodeMigration, {
    enabled: canStartDataNode,
  });

  const { currentStep } = useMigrationState({ enabled: canStartDataNode });
  const noMigrationInProgress = !currentStep || currentStep?.state === 'NEW' || currentStep?.state === 'FINISHED';

  return {
    isDatanodeConfiguredAndUsed: canStartDataNode && !!isDatanodeConfiguredAndUsed,
    showDatanodeMigration: canStartDataNode && (!isDatanodeConfiguredAndUsed || !noMigrationInProgress),
  };
};

export default useShowDatanodeMigration;
