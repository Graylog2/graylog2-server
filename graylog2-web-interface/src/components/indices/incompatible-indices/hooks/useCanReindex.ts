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

import { Datanode } from '@graylog/server-api';

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';

const STALE_TIME_MS = 300000;

/**
 * Reindexing a system index needs the admin certificate that only a Data Node search backend accepts.
 * Unless we know for sure that this Graylog does not run against Data Nodes, we keep the action available.
 */
const useCanReindex = (): boolean => {
  const { permissions } = useCurrentUser();

  const { data: runsWithDataNode } = useQuery({
    queryKey: ['datanode', 'configured'],
    queryFn: () => Datanode.runsWithDataNode({ requestShouldExtendSession: false }),
    enabled: isPermitted(permissions, 'datanode:read'),
    staleTime: STALE_TIME_MS,
  });

  return runsWithDataNode !== false;
};

export default useCanReindex;
