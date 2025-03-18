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
    () => ({ enabled: true, interval: durationToMS(defaultInterval) }),
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
