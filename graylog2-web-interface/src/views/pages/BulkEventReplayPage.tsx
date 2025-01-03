import * as React from 'react';
import { useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import useLocation from 'routing/useLocation';
import Spinner from 'components/common/Spinner';
import BulkEventReplay from 'components/events/bulk-replay/BulkEventReplay';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';

export type BulkEventReplayState = {
  eventIds: Array<string>;
}

const useEventsById = (eventIds: Array<string>) => useQuery(['events', eventIds], () => Events.getByIds({ event_ids: eventIds }));

const BulkEventReplayPage = () => {
  const location = useLocation<BulkEventReplayState>();
  const initialEventIds = location.state?.eventIds ?? [];
  const { data: events, isInitialLoading } = useEventsById(initialEventIds);

  const history = useHistory();
  const onClose = useCallback(() => {
    history.push(Routes.ALERTS.LIST);
  }, [history]);

  return isInitialLoading
    ? <Spinner />
    : <BulkEventReplay events={events} initialEventIds={initialEventIds} onClose={onClose} />;
};

export default BulkEventReplayPage;
