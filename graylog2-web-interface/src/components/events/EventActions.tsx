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
import { useRef } from 'react';

import { ButtonToolbar, MenuItem } from 'components/bootstrap';
import { MoreActions } from 'components/common/EntityDataTable';
import LinkToReplaySearch from 'components/event-definitions/replay-search/LinkToReplaySearch';
import usePluginEntities from 'hooks/usePluginEntities';
import type { Event } from 'components/events/events/types';

const usePluggableEventActions = (eventId: string) => {
  const modalRefs = useRef({});
  const pluggableActions = usePluginEntities('views.components.eventActions');
  const availableActions = pluggableActions.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  );
  const actions = availableActions.map(({ component: PluggableEventAction, key }) => (
    <PluggableEventAction key={`event-action-${key}`}
                          eventId={eventId}
                          modalRef={() => modalRefs.current[key]} />
  ));

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal key={`event-action-modal-${key}`}
                   eventId={eventId}
                   ref={(r) => { modalRefs.current[key] = r; }} />
    ));

  return ({ actions, actionModals });
};

const EventActions = ({ event }: { event: Event }) => {
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableEventActions(event.id);

  const moreActions = [
    event.replay_info ? <MenuItem><LinkToReplaySearch id={event.id} isEvent /></MenuItem> : null,
    event.replay_info && pluggableActions.length ? <MenuItem divider key="divider" /> : null,
    pluggableActions.length ? pluggableActions : null,
  ].filter(Boolean);

  return (
    <>
      <ButtonToolbar>
        <MoreActions>
          {moreActions}
        </MoreActions>
      </ButtonToolbar>
      {pluggableActionModals}
    </>
  );
};

export default EventActions;
