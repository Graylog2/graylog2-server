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

import { ButtonToolbar, MenuItem } from 'components/bootstrap';
import { MoreActions } from 'components/common/EntityDataTable';
import LinkToReplaySearch from 'components/event-definitions/replay-search/LinkToReplaySearch';
import type { Event } from 'components/events/events/types';
import usePluggableEventActions from 'components/events/events/hooks/usePluggableEventActions';

const DefaultWrapper = ({ children }: React.PropsWithChildren) => (
  <ButtonToolbar>
    <MoreActions>
      {children}
    </MoreActions>
  </ButtonToolbar>
);

const EventActions = ({ event, wrapper: Wrapper = DefaultWrapper }: { event: Event, wrapper?: React.ComponentType<React.PropsWithChildren> }) => {
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableEventActions(event.id);
  const hasReplayInfo = !!event.replay_info;

  const moreActions = [
    hasReplayInfo ? <MenuItem key="replay_info"><LinkToReplaySearch id={event.id} isEvent /></MenuItem> : null,
    pluggableActions.length ? <MenuItem divider key="divider" /> : null,
    pluggableActions.length ? pluggableActions : null,
  ].filter(Boolean);

  return moreActions.length ? (
    <>
      <Wrapper>
        {moreActions}
      </Wrapper>
      {pluggableActionModals}
    </>
  ) : null;
};

export default EventActions;
