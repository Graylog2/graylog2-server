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
import { useMemo } from 'react';

import useDefaultInterval from 'views/hooks/useDefaultIntervalForRefresh';
import type { RefreshConfig } from 'views/components/contexts/AutoRefreshContext';
import { durationToMS } from 'util/DateTime';
import AutoRefreshProvider from 'views/components/contexts/AutoRefreshProvider';

import Spinner from '../common/Spinner';

const noop = () => {};

const EventsRefreshProvider = ({ children = undefined }: React.PropsWithChildren<{}>) => {
  const defaultInterval = useDefaultInterval();
  const defaultRefreshConfig: RefreshConfig = useMemo(
    () => ({ enabled: false, interval: durationToMS(defaultInterval) }),
    [defaultInterval],
  );

  return defaultInterval === null ? (
    <Spinner />
  ) : (
    <AutoRefreshProvider onRefresh={noop} defaultRefreshConfig={defaultRefreshConfig}>
      {children}
    </AutoRefreshProvider>
  );
};
export default EventsRefreshProvider;
