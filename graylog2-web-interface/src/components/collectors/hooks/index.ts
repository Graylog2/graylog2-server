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
export {
  useFleets,
  useFleet,
  useFleetStats,
  useFleetsBulkStats,
  fetchPaginatedFleets,
  fleetsKeyFn,
  FLEETS_KEY_PREFIX,
} from './useFleetQueries';

export { useInstances, fetchPaginatedInstances, instancesKeyFn, INSTANCES_KEY_PREFIX } from './useInstanceQueries';

export { useSources, fetchPaginatedSources, sourcesKeyFn, SOURCES_KEY_PREFIX } from './useSourceQueries';

export { useCollectorStats } from './useCollectorStats';
export { useCollectorsConfig } from './useCollectorsConfig';
export { useCollectorInputIds } from './useCollectorInputIds';

export {
  fetchPaginatedEnrollmentTokens,
  enrollmentTokensKeyFn,
  ENROLLMENT_TOKENS_KEY_PREFIX,
} from './useEnrollmentTokenQueries';

export { default as useCollectorsMutations } from './useCollectorsMutations';
export { default as useDefaultInstanceFilters } from './useDefaultInstanceFilters';

export { useRecentActivity, ACTIVITY_KEY } from './useActivityQueries';
