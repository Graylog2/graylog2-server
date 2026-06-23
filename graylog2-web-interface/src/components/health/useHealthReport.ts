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
import { useQuery, useQueryClient } from '@tanstack/react-query';
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
 * Conditional poll for the cluster health report.
 *
 * The backend advances `generated_at` only when content changes, and returns 204 when our echoed
 * `?since=` still matches. We therefore echo the last `generated_at` verbatim (never round-tripped
 * through a Date, which would alter precision/offset and miss every 204) and, on 204, keep the
 * previously fetched report. Both consumers (panel + nav badge) share one query via the query key,
 * so the last report read from the cache is the single source of truth for `?since=`.
 */
const fetchHealthReport = async (previous: HealthReport | undefined): Promise<HealthReport> => {
  const since = previous?.generated_at;
  const url = since ? `${HEALTH_REPORT_URL}?since=${encodeURIComponent(since)}` : HEALTH_REPORT_URL;

  // Default fetch returns the parsed body on 200 and `null` on 204; it throws on 4xx/5xx.
  const report: HealthReport | null = await fetch('GET', qualifyUrl(url));

  if (report === null) {
    // Request-binding rule: a 204 is only "unchanged" when we actually sent ?since=. An unsolicited
    // 204 (no since) is a contract violation, not a benign "nothing changed" -- surface it as an error.
    if (!since) {
      throw new Error('Received an unsolicited 204 from the health endpoint (no ?since= was sent).');
    }

    return previous;
  }

  return report;
};

const useHealthReport = (): UseQueryResult<HealthReport> => {
  const queryClient = useQueryClient();
  // Only poll when the feature is enabled, so we don't hit the (license/permission-gated) endpoint for
  // every user. Mirrors the visibility gate the consumers already apply before rendering.
  const enabled = useHealthModuleVisible();

  return useQuery({
    queryKey: HEALTH_REPORT_QUERY_KEY,
    queryFn: () => fetchHealthReport(queryClient.getQueryData<HealthReport>(HEALTH_REPORT_QUERY_KEY)),
    refetchInterval: POLL_INTERVAL_MS,
    enabled,
  });
};

export default useHealthReport;
