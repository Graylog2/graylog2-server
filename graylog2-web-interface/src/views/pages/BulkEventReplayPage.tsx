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
// @ts-nocheck
import * as React from 'react';
import { useCallback } from 'react';
import { useQuery } from '@tanstack/react-query';

import { Events } from '@graylog/server-api';

import Store from 'logic/local-storage/Store';
import useLocation from 'routing/useLocation';
import Spinner from 'components/common/Spinner';
import BulkEventReplay from 'components/events/bulk-replay/BulkEventReplay';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import type { RemainingBulkActionsProps } from 'components/events/bulk-replay/types';
import RemainingBulkActions from 'components/events/bulk-replay/RemainingBulkActions';
import { singleton } from 'logic/singleton';
import { REPLAY_SESSION_ID_PARAM } from 'components/events/Constants';
import useRoutingQuery from 'routing/useQuery';

export type BulkEventReplayState = {
  eventIds: Array<string>;
  returnUrl: string;
};

const useEventsById = (eventIds: Array<string>) =>
  useQuery({
    queryKey: ['events', eventIds],
    queryFn: () => Events.getByIds({ event_ids: eventIds }),
    enabled: !!eventIds,
  });

type Props = {
  BulkActions?: React.ComponentType<RemainingBulkActionsProps>;
};

const BulkEventReplayPage = ({ BulkActions = RemainingBulkActions }: Props) => {
  const location = useLocation<BulkEventReplayState>();
  const { returnUrl } = location?.state ?? {};
  const params = useRoutingQuery();
  const replaySessionId = params[REPLAY_SESSION_ID_PARAM];

  const initialEventIds: Array<string> = Store.sessionGet(replaySessionId);
  const { data: events, isFetched } = useEventsById(initialEventIds);

  const history = useHistory();
  const onClose = useCallback(() => {
    history.push(returnUrl ?? Routes.ALERTS.LIST);
  }, [history, returnUrl]);

  return initialEventIds && isFetched ? (
    <BulkEventReplay events={events} initialEventIds={initialEventIds} onClose={onClose} BulkActions={BulkActions} />
  ) : (
    <Spinner />
  );
};

export default singleton('pages.BulkEventReplayPage', () => BulkEventReplayPage);
