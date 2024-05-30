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
import { useMemo, useState, useEffect, useCallback } from 'react';

import type { RefreshConfig, RefreshCallback } from 'views/components/contexts/AutoRefreshContext';
import AutoRefreshContext from 'views/components/contexts/AutoRefreshContext';
import useAppDispatch from 'stores/useAppDispatch';
import { execute } from 'views/logic/slices/searchExecutionSlice';
import useAppSelector from 'stores/useAppSelector';
import { selectJobIds } from 'views/logic/slices/searchExecutionSelectors';

const AutoRefreshProvider = ({ children }: React.PropsWithChildren) => {
  const dispatch = useAppDispatch();
  const jobIds = useAppSelector(selectJobIds);
  const [callbacks, setCallbacks] = useState<Record<string, RefreshCallback>>({});
  const [refreshConfig, setRefreshConfig] = useState<RefreshConfig | null>(null);
  const startAutoRefresh = useCallback((interval: number) => setRefreshConfig({ enabled: true, interval }), []);
  const stopAutoRefresh = useCallback(() => setRefreshConfig((cur) => ({ ...cur, enabled: false })), []);

  const refreshSearch = useCallback(() => {
    if (!jobIds) {
      dispatch(execute());
    }
  }, [jobIds, dispatch]);

  useEffect(() => {
    console.log(jobIds);
    const refreshInterval = refreshConfig?.enabled && !jobIds
      ? setInterval(() => {
        console.log('refresh');
        refreshSearch();
        Object.values(callbacks).forEach((callback) => callback());
      }, refreshConfig.interval)
      : null;

    return () => {
      clearInterval(refreshInterval);
      console.log('refresh2');
      Object.values(callbacks).forEach((callback) => callback());
    };
  }, [jobIds?.asyncSearchId, refreshSearch, refreshConfig?.enabled, refreshConfig?.interval, callbacks]);

  const registerCallback = useCallback((callback: RefreshCallback, id: string) => {
    setCallbacks((prevCallbacks) => ({ ...prevCallbacks, [id]: callback }));
  }, []);

  const unregisterCallback = useCallback((id: string) => {
    setCallbacks((prevCallbacks) => {
      const newCallbacks = { ...prevCallbacks };
      delete newCallbacks[id];

      return newCallbacks;
    });
  }, []);

  const contextValue = useMemo(() => ({
    refreshConfig,
    startAutoRefresh,
    stopAutoRefresh,
    registerCallback,
    unregisterCallback,
  }), [
    refreshConfig,
    registerCallback,
    startAutoRefresh,
    stopAutoRefresh,
    unregisterCallback,
  ]);

  return (
    <AutoRefreshContext.Provider value={contextValue}>
      {children}
    </AutoRefreshContext.Provider>
  );
};

export default AutoRefreshProvider;
