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
