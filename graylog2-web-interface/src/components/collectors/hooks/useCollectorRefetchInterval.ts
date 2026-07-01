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
import moment from 'moment';

import { useCollectorsConfig } from './useCollectorsConfig';

// Fallback used until the collectors config (and thus the heartbeat interval) has loaded.
const DEFAULT_REFETCH_INTERVAL_MS = 30_000;

/**
 * Polling cadence for the instances table and the activity / pending-changes views. Tied to the
 * collector heartbeat interval so the "Pending" badge (and status/last_seen) clear on their own
 * once a collector has applied its changes — and stays correct if the heartbeat interval changes.
 */
const useCollectorRefetchInterval = (): number => {
  const { data: config } = useCollectorsConfig();

  if (!config?.collector_heartbeat_interval) {
    return DEFAULT_REFETCH_INTERVAL_MS;
  }

  const intervalMs = moment.duration(config.collector_heartbeat_interval).asMilliseconds();

  return intervalMs > 0 ? intervalMs : DEFAULT_REFETCH_INTERVAL_MS;
};

export default useCollectorRefetchInterval;
