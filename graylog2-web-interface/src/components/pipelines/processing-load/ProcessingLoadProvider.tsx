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
import React, { createContext, useContext } from 'react';

import DebugMetricsBanner from 'components/pipelines/debug-metrics/DebugMetricsBanner';
import useDebugMetricsConfig from 'components/pipelines/debug-metrics/useDebugMetricsConfig';
import type { UseDebugMetricsConfig } from 'components/pipelines/debug-metrics/useDebugMetricsConfig';

import type { ProcessingLoadResponse } from './types';
import useProcessingLoad from './useProcessingLoad';

type ProcessingLoadContextValue = {
  debugMetricsConfig: UseDebugMetricsConfig;
  metricsEnabled: boolean;
  processingLoad?: ProcessingLoadResponse;
  processingLoadError: boolean;
};

type Props = {
  children: React.ReactNode;
  enabled?: boolean;
};

const disabledDebugMetricsConfig: UseDebugMetricsConfig = {
  metricsEnabled: false,
  isLoading: false,
  refresh: () => Promise.resolve(),
  disable: () => Promise.resolve(),
};

const disabledProcessingLoadContext: ProcessingLoadContextValue = {
  debugMetricsConfig: disabledDebugMetricsConfig,
  metricsEnabled: false,
  processingLoad: undefined,
  processingLoadError: false,
};

const ProcessingLoadContext = createContext<ProcessingLoadContextValue | undefined>(undefined);

export const useProcessingLoadContext = (): ProcessingLoadContextValue =>
  useContext(ProcessingLoadContext) ?? disabledProcessingLoadContext;

export const ProcessingLoadProvider = ({ children, enabled = true }: Props) => {
  const debugMetricsConfig = useDebugMetricsConfig();
  const { metricsEnabled } = debugMetricsConfig;
  const { data: processingLoad, isError: processingLoadError } = useProcessingLoad({
    enabled: enabled && metricsEnabled,
  });

  return (
    <ProcessingLoadContext.Provider
      value={{
        debugMetricsConfig,
        metricsEnabled,
        processingLoad,
        processingLoadError,
      }}>
      {children}
    </ProcessingLoadContext.Provider>
  );
};

export const ProcessingLoadDebugMetricsBanner = () => {
  const { debugMetricsConfig } = useProcessingLoadContext();

  return <DebugMetricsBanner config={debugMetricsConfig} />;
};
