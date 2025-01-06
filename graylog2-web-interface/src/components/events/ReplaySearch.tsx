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

import useAlertAndEventDefinitionData
  from 'components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData';
import useCreateViewForEventDefinition from 'views/logic/views/UseCreateViewForEventDefinition';
import useCreateSearch from 'views/hooks/useCreateSearch';
import EventInfoBar from 'components/event-definitions/replay-search/EventInfoBar';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import ReplaySearchContext from 'components/event-definitions/replay-search/ReplaySearchContext';

type Props = {
  alertId: string,
  definitionId: string,
  replayEventDefinition?: boolean,
  searchPageLayout?: React.ComponentProps<typeof SearchPageLayoutProvider>['value'],
  forceSidebarPinned?: boolean,
}

const defaultSearchPageLayout = {};

const ReplaySearch = ({
  alertId, definitionId, replayEventDefinition = false,
  searchPageLayout = defaultSearchPageLayout, forceSidebarPinned = false,
}: Props) => {
  const { eventDefinition, aggregations, eventData } = useAlertAndEventDefinitionData(alertId, definitionId);
  const _view = useCreateViewForEventDefinition({ eventDefinition, aggregations });
  const view = useCreateSearch(_view);
  const _searchPageLayout = useMemo(() => ({
    ...searchPageLayout,
    infoBar: { component: EventInfoBar },
  }), []);
  const replaySearchContext = useMemo(() => ({
    alertId,
    definitionId,
    // eslint-disable-next-line no-nested-ternary
    type: replayEventDefinition ? 'event_definition' : eventData?.alert ? 'alert' : 'event',
  } as const), [alertId, definitionId, eventData?.alert, replayEventDefinition]);

  return (
    <ReplaySearchContext.Provider value={replaySearchContext}>
      <SearchPageLayoutProvider value={_searchPageLayout}>
        <SearchPage view={view}
                    isNew
                    forceSideBarPinned={forceSidebarPinned} />
      </SearchPageLayoutProvider>
    </ReplaySearchContext.Provider>
  );
};

export default ReplaySearch;
