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

import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import type { Event } from 'components/events/events/types';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';
import type { BulkEventReplayState } from 'views/pages/BulkEventReplayPage';
import useLocation from 'routing/useLocation';

type Props = {
  selectedEntitiesData: { [eventId: string]: Event }
}

const BulkActions = ({ selectedEntitiesData }: Props) => {
  const events = Object.values(selectedEntitiesData);
  const { actions, pluggableActionModals } = useEventBulkActions(events);

  const location = useLocation();
  const returnUrl = `${location.pathname}${location.search}`;

  const history = useHistory();
  const onReplaySearchClick = useCallback(() => {
    const eventIds = events.map((event) => event.id);
    history.pushWithState<BulkEventReplayState>(Routes.ALERTS.BULK_REPLAY_SEARCH, { eventIds, returnUrl });
  }, [events, history, returnUrl]);

  return (
    <>
      <BulkActionsDropdown>
        <MenuItem onClick={onReplaySearchClick}>Replay Search</MenuItem>
        {actions}
      </BulkActionsDropdown>
      {pluggableActionModals}
    </>
  );
};

export default BulkActions;
