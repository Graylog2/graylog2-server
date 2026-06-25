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
import type { UseQueryResult } from '@tanstack/react-query';

import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

import type { HealthReport } from './HealthReport.types';
import useHealthModuleVisible from './useHealthModuleVisible';

// Pinned literal path -- must match the backend resource (HealthReportResource @Path + HealthModule package).
const HEALTH_REPORT_URL = '/plugins/org.graylog.plugins.health/cluster';
const HEALTH_REPORT_QUERY_KEY = ['health', 'cluster-report'] as const;
const POLL_INTERVAL_MS = 5000;

/**
 * Poll for the cluster health report.
 *
 * The backend assembles a fresh full report on every request, so each poll returns a complete `200`
 * body -- there is no conditional polling: no `?since=`, no `204`, no version token. `generated_at` is
 * a display timestamp the backend stamps at assembly time; we render it but never echo it back.
 */
const fetchHealthReport = (): Promise<HealthReport> => fetch('GET', qualifyUrl(HEALTH_REPORT_URL));

const useHealthReport = (): UseQueryResult<HealthReport> => {
  // Only poll when the feature is enabled, so we don't hit the (license/permission-gated) endpoint for
  // every user. Mirrors the visibility gate the consumers already apply before rendering.
  const enabled = useHealthModuleVisible();

  return useQuery({
    queryKey: HEALTH_REPORT_QUERY_KEY,
    queryFn: fetchHealthReport,
    refetchInterval: POLL_INTERVAL_MS,
    enabled,
  });
};

export default useHealthReport;
