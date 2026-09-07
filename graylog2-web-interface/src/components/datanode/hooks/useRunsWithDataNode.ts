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

const DATANODE_CONFIGURED_QUERY_KEY = ['datanode', 'configured'] as const;
const STALE_TIME_MS = 300000;

type Options = {
  enabled?: boolean;
};

type Result = {
  data: boolean | undefined;
  isLoading: boolean;
};

const useRunsWithDataNode = ({ enabled = true }: Options = {}): Result => {
  const { permissions } = useCurrentUser();
  const { data, isLoading } = useQuery({
    queryKey: DATANODE_CONFIGURED_QUERY_KEY,
    queryFn: () => Datanode.runsWithDataNode({ requestShouldExtendSession: false }),
    enabled: enabled && isPermitted(permissions, 'datanode:read'),
    staleTime: STALE_TIME_MS,
  });

  return { data, isLoading };
};

export default useRunsWithDataNode;
