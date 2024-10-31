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
import { useCallback } from 'react';

import { ClusterSystemLoggers } from '@graylog/server-api';

const _setSubsystemLoggerLevel = (args: { nodeId: string, name: string, level: string }) => ClusterSystemLoggers.setSubsystemLoggerLevel(args.nodeId, args.name, args.level);

const useSetSubsystemLoggerLevel = () => {
  const queryClient = useQueryClient();
  const { mutateAsync, isLoading } = useMutation(_setSubsystemLoggerLevel, {
    onSuccess: () => {
      queryClient.invalidateQueries(['loggers']);
    },
  });
  const setSubsystemLoggerLevel = useCallback((nodeId: string, name: string, level: string) => mutateAsync({ nodeId, name, level }), [mutateAsync]);

  return { setSubsystemLoggerLevel, isLoading };
};

export default useSetSubsystemLoggerLevel;
