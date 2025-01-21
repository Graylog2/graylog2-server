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
  returnUrl: string;
}

const useEventsById = (eventIds: Array<string>) => useQuery(['events', eventIds], () => Events.getByIds({ event_ids: eventIds }));

const BulkEventReplayPage = () => {
  const location = useLocation<BulkEventReplayState>();
  const { eventIds: initialEventIds = [], returnUrl } = (location?.state ?? {});
  const { data: events, isInitialLoading } = useEventsById(initialEventIds);

  const history = useHistory();
  const onClose = useCallback(() => {
    history.push(returnUrl ?? Routes.ALERTS.LIST);
  }, [history, returnUrl]);

  return isInitialLoading
    ? <Spinner />
    : <BulkEventReplay events={events} initialEventIds={initialEventIds} onClose={onClose} />;
};

export default BulkEventReplayPage;
