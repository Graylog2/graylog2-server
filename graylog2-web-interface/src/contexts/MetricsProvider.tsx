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
import * as React from 'react';
import { useCallback, useMemo, useState } from 'react';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

import { fetchPeriodically } from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import MetricsContext from 'contexts/MetricsContext';
import type { ClusterMetric, Metric } from 'types/metrics';

const buildMetricsFromResponse = (response: Record<string, { metrics: Metric[] } | null>): ClusterMetric => {
  const metrics: ClusterMetric = {};

  Object.keys(response).forEach((nodeId) => {
    if (!response[nodeId]) {
      return;
    }

    const nodeMetrics: Record<string, Metric> = {};

    response[nodeId].metrics.forEach((metric) => {
      nodeMetrics[metric.full_name] = metric;
    });

    metrics[nodeId] = nodeMetrics;
  });

  return metrics;
};

const MetricsProvider = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const [subscriptions, setSubscriptions] = useState<Record<string, number>>({});

  const subscribedNames = useMemo(
    () => Object.keys(subscriptions).filter((name) => subscriptions[name] > 0).sort(),
    [subscriptions],
  );

  const subscribe = useCallback((names: string[]) => {
    setSubscriptions((prev) => {
      const next = { ...prev };

      names.forEach((name) => {
        next[name] = (next[name] || 0) + 1;
      });

      return next;
    });
  }, []);

  const unsubscribe = useCallback((names: string[]) => {
    setSubscriptions((prev) => {
      const next = { ...prev };

      names.forEach((name) => {
        if (next[name] !== undefined) {
          next[name] -= 1;

          if (next[name] <= 0) {
            delete next[name];
          }
        }
      });

      return next;
    });
  }, []);

  const { data: metrics, isLoading } = useQuery<ClusterMetric>({
    queryKey: ['metrics', 'cluster', subscribedNames],
    queryFn: () => {
      const url = qualifyUrl(ApiRoutes.ClusterMetricsApiController.multipleAllNodes().url);

      return fetchPeriodically<Record<string, { metrics: Metric[] } | null>>(
        'POST',
        url,
        { metrics: subscribedNames },
      ).then(buildMetricsFromResponse);
    },
    enabled: subscribedNames.length > 0,
    refetchInterval: 2000,
    placeholderData: keepPreviousData,
  });

  const value = useMemo(() => ({
    metrics: metrics ?? {},
    isLoading,
    subscribe,
    unsubscribe,
  }), [metrics, isLoading, subscribe, unsubscribe]);

  return (
    <MetricsContext.Provider value={value}>
      {children}
    </MetricsContext.Provider>
  );
};

export default MetricsProvider;
