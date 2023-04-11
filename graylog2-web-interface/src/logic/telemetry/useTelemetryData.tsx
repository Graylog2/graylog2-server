import { useQuery } from '@tanstack/react-query';

import { Telemetry } from '@graylog/server-api';

const TELEMETRY_CLUSTER_INFO_QUERY_KEY = 'telemetry.cluster.info';

export type TelemetryDataType = {
  current_user?: {
    [key: string]: string,
  },
  user_telemetry_settings?: {
    [key: string]: boolean,
  },
  cluster?: {
    [key: string]: string,
  },
  license?: {
    [key: string]: string,
  },
  plugin?: {
    [key: string]: string,
  },
  search_cluster?: {
    [key: string]: string,
  },
}

const useTelemetryData = () => {
  return useQuery([TELEMETRY_CLUSTER_INFO_QUERY_KEY], () => Telemetry.get() as Promise<TelemetryDataType>, {
    retry: 0,
    keepPreviousData: true,
    notifyOnChangeProps: ['data', 'error'],
  });
};

export default useTelemetryData;
