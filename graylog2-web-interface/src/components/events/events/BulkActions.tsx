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

import MenuItem from 'components/bootstrap/menuitem/MenuItem';
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';
import type { Event } from 'components/events/events/types';
import useEventBulkActions from 'components/events/events/hooks/useEventBulkActions';
import { isPermitted } from 'util/PermissionsMixin';
import useCurrentUser from 'hooks/useCurrentUser';
import useReplayBulkAction from 'components/events/events/hooks/useReplayBulkAction';

type Props = {
  selectedEntitiesData: { [eventId: string]: Event };
};

const BulkActions = ({ selectedEntitiesData }: Props) => {
  const events = Object.values(selectedEntitiesData);
  const { actions, pluggableActionModals } = useEventBulkActions(events);
  const currentUser = useCurrentUser();

  const replayableEvents = events.filter(
    (event) => !!event.replay_info && isPermitted(currentUser?.permissions, `eventdefinitions:read:${event.id}`),
  );
  const onReplaySearchClick = useReplayBulkAction(replayableEvents);

  return (
    <>
      <BulkActionsDropdown>
        {replayableEvents.length > 0 && <MenuItem onClick={onReplaySearchClick}>Replay Search</MenuItem>}
        {actions}
      </BulkActionsDropdown>
      {pluggableActionModals}
    </>
  );
};

export default BulkActions;
