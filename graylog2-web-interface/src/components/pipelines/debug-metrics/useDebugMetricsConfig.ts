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
import { useQueryClient } from '@tanstack/react-query';

import {
  useRuleMetricsConfig,
  updateRuleMetricsConfig,
  RULE_METRICS_CONFIG_QUERY_KEY,
} from 'components/rules/hooks/useRules';
import type { MetricsConfigType } from 'components/rules/hooks/useRules';

export type UseDebugMetricsConfig = {
  metricsEnabled: boolean;
  isLoading: boolean;
  refresh: () => Promise<unknown>;
  disable: () => Promise<unknown>;
};

type Options = {
  loadOnMount?: boolean;
};

const useDebugMetricsConfig = ({ loadOnMount = true }: Options = {}): UseDebugMetricsConfig => {
  const queryClient = useQueryClient();
  const { data: metricsConfig, isInitialLoading, refetch } = useRuleMetricsConfig({ enabled: loadOnMount });

  return {
    metricsEnabled: !!metricsConfig?.metrics_enabled,
    isLoading: loadOnMount && isInitialLoading,
    refresh: () => refetch(),
    disable: () =>
      updateRuleMetricsConfig({ metrics_enabled: false } as MetricsConfigType).then((response) => {
        queryClient.invalidateQueries({ queryKey: RULE_METRICS_CONFIG_QUERY_KEY });

        return response;
      }),
  };
};

export default useDebugMetricsConfig;
