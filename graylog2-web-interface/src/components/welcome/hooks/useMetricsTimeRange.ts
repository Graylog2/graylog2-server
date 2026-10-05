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
import { durationInSeconds } from 'util/DateTime';

import { DEFAULT_TIME_RANGE_SECONDS } from './useWelcomeSearch';

/**
 * Caps the welcome page's default 24-hour widget time range to the deployment's configured query
 * time range limit, when a limit is set and it is smaller than 24 hours.
 */
const useMetricsTimeRange = () => {
  const { config } = useSearchConfiguration();
  const queryTimeRangeLimitSeconds = durationInSeconds(config?.query_time_range_limit ?? '');

  return queryTimeRangeLimitSeconds > 0 && queryTimeRangeLimitSeconds < DEFAULT_TIME_RANGE_SECONDS
    ? queryTimeRangeLimitSeconds
    : DEFAULT_TIME_RANGE_SECONDS;
};

export default useMetricsTimeRange;
