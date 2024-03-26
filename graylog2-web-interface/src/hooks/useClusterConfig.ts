import { useQuery } from '@tanstack/react-query';

import { SystemClusterConfig } from '@graylog/server-api';

const useClusterConfig = <T, >(key: string) => useQuery<T>(['system', 'cluster_config', key], () => SystemClusterConfig.read(key) as Promise<T>);

export default useClusterConfig;
