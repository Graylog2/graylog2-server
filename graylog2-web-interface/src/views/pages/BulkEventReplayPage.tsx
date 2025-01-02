import * as React from 'react';
import { useQuery } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import useLocation from 'routing/useLocation';
import Spinner from 'components/common/Spinner';
import BulkEventReplay from 'components/events/bulk-replay/BulkEventReplay';

export type BulkEventReplayState = {
  eventIds: Array<string>;
}

const useEventsById = (eventIds: Array<string>) => useQuery(['events', eventIds], () => Events.getByIds({ event_ids: eventIds }));

const BulkEventReplayPage = () => {
  const location = useLocation<BulkEventReplayState>();
  const initialEventIds = location.state.eventIds ?? [];
  const { data: events, isInitialLoading } = useEventsById(initialEventIds);

  return isInitialLoading
    ? <Spinner />
    : <BulkEventReplay events={events} initialEventIds={initialEventIds} />;
};

export default BulkEventReplayPage;
