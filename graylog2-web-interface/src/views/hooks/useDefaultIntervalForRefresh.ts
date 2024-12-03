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

import useSearchConfiguration from 'hooks/useSearchConfiguration';
import useMinimumRefreshInterval from 'views/hooks/useMinimumRefreshInterval';
import { durationToMS } from 'util/DateTime';

const useDefaultIntervalForRefresh = () => {
  const { config } = useSearchConfiguration();
  const { data: minimumInterval } = useMinimumRefreshInterval();
  const minimumIntervalInMS = durationToMS(minimumInterval);

  const autoRefreshTimerangeOptions = config?.auto_refresh_timerange_options;
  const defaultAutoRefreshInterval = config?.default_auto_refresh_option;

  if (!config || !autoRefreshTimerangeOptions || !defaultAutoRefreshInterval) return null;

  if (durationToMS(defaultAutoRefreshInterval) < minimumIntervalInMS) {
    const availableIntervals = Object.entries(autoRefreshTimerangeOptions)
      .filter(([interval]) => durationToMS(interval) >= minimumIntervalInMS)
      .sort(([interval1], [interval2]) => (
        durationToMS(interval1) > durationToMS(interval2) ? 1 : -1
      ));

    return availableIntervals.length ? availableIntervals[0][0] : null;
  }

  return defaultAutoRefreshInterval;
};

export default useDefaultIntervalForRefresh;
