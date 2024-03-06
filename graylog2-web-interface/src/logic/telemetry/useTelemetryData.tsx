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
  data_nodes?: {
    data_nodes_count: number,
  }
}

const useTelemetryData = () => useQuery([TELEMETRY_CLUSTER_INFO_QUERY_KEY], () => Telemetry.get() as Promise<TelemetryDataType>, {
  retry: 0,
  keepPreviousData: true,
  notifyOnChangeProps: ['data', 'error'],
});

export default useTelemetryData;
