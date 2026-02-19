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

import Store from 'logic/local-storage/Store';
import Spinner from 'components/common/Spinner';
import BulkEventReplay from 'components/events/bulk-replay/BulkEventReplay';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import { singleton } from 'logic/singleton';
import { REPLAY_SESSION_ID_PARAM } from 'components/events/Constants';
import useRoutingQuery from 'routing/useQuery';
import useSessionInitialEventIds from 'components/events/bulk-replay/hooks/useSessionInitialEventIds';
import useEventsById from 'components/events/bulk-replay/hooks/useEventsById';

const BulkEventReplayPage = () => {
  const params = useRoutingQuery();
  const replaySessionId = params[REPLAY_SESSION_ID_PARAM];
  const initialEventIds = useSessionInitialEventIds();
  const returnUrl: string = Store.sessionGet(replaySessionId as string)?.returnUrl;
  const { data: events, isFetched } = useEventsById(initialEventIds);

  const history = useHistory();
  const onReturnClick = useCallback(() => {
    history.push(returnUrl ?? Routes.ALERTS.LIST);
  }, [history, returnUrl]);

  return initialEventIds && isFetched ? (
    <BulkEventReplay events={events} initialEventIds={initialEventIds} onReturnClick={onReturnClick} />
  ) : (
    <Spinner />
  );
};

export default singleton('pages.BulkEventReplayPage', () => BulkEventReplayPage);
