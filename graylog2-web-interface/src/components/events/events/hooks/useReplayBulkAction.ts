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
import { useCallback, useMemo } from 'react';
import URI from 'urijs';

import useSendEventActionTelemetry from 'components/events/events/hooks/useSendEventActionTelemetry';
import type { Event } from 'components/events/events/types';
import Routes from 'routing/Routes';
import Store from 'logic/local-storage/Store';
import generateId from 'logic/generateId';
import { REPLAY_SESSION_ID_PARAM } from 'components/events/Constants';
import useLocation from 'routing/useLocation';

const useReplayBulkAction = (replayableEvents: Array<Event>) => {
  const sendEventActionTelemetry = useSendEventActionTelemetry();
  const eventIds = useMemo(() => replayableEvents.map((event) => event.id), [replayableEvents]);
  const { pathname, search } = useLocation();

  return useCallback(() => {
    const sessionId = generateId();
    const url = new URI(Routes.ALERTS.BULK_REPLAY_SEARCH)
      .search({
        [REPLAY_SESSION_ID_PARAM]: sessionId,
      })
      .toString();
    sendEventActionTelemetry('REPLAY_SEARCH', true, { events_length: eventIds.length });
    Store.sessionSet(sessionId, { initialEventIds: eventIds, returnUrl: `${pathname}${search}` });
    window.open(url, '_blank');
  }, [eventIds, pathname, search, sendEventActionTelemetry]);
};

export default useReplayBulkAction;
