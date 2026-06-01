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
import { useEffect } from 'react';

import { useStore } from 'stores/connect';
import { RulesActions, RulesStore } from 'stores/rules/RulesStore';
import type { MetricsConfigType } from 'stores/rules/RulesStore';

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
  const metricsConfig = useStore(RulesStore, (state) => state?.metricsConfig);

  useEffect(() => {
    if (loadOnMount) {
      RulesActions.loadMetricsConfig();
    }
  }, [loadOnMount]);

  return {
    metricsEnabled: !!metricsConfig?.metrics_enabled,
    isLoading: metricsConfig === undefined,
    refresh: () => RulesActions.loadMetricsConfig(),
    disable: () => RulesActions.updateMetricsConfig({ metrics_enabled: false } as MetricsConfigType),
  };
};

export default useDebugMetricsConfig;
