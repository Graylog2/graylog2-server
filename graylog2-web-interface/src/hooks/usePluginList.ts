import { useQuery } from '@tanstack/react-query';

import { ClusterPlugins } from '@graylog/server-api';

const usePluginList = (nodeId: string) => {
  const { data, isInitialLoading } = useQuery(['plugins', 'list', nodeId], () => ClusterPlugins.list(nodeId));

  return { pluginList: data, isLoading: isInitialLoading };
};

export default usePluginList;
