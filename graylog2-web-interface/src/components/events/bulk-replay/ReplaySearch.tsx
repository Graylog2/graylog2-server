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

import useCreateSearch from 'views/hooks/useCreateSearch';
import EventInfoBar from 'components/event-definitions/replay-search/EventInfoBar';
import SearchPageLayoutProvider from 'views/components/contexts/SearchPageLayoutProvider';
import SearchPage from 'views/pages/SearchPage';
import useCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import useEventDefinition from 'hooks/useEventDefinition';
import Spinner from 'components/common/Spinner';
import type { Event } from 'components/events/events/types';

type Props = {
  event: Event;
}

const ViewReplaySearch = ({ eventData, eventDefinition, aggregations }) => {
  const _view = useCreateViewForEvent({ eventData, eventDefinition, aggregations });
  const view = useCreateSearch(_view);
  const searchPageLayout = useMemo(() => ({
    sidebar: {
      isShown: false,
    },
    infoBar: { component: EventInfoBar },
    synchronizeUrl: false,
  } as const), []);

  return (
    <SearchPageLayoutProvider value={searchPageLayout}>
      <SearchPage view={view} isNew />
    </SearchPageLayoutProvider>
  );
};

const ReplaySearch = ({ event }: Props) => {
  const { data, isLoading } = useEventDefinition(event.event_definition_id);

  return isLoading ? <Spinner />
    : <ViewReplaySearch eventData={event} eventDefinition={data?.eventDefinition} aggregations={data?.aggregations} />;
};

export default ReplaySearch;
