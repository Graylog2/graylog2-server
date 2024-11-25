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
import React, { useRef } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { EventActionComponentProps } from 'views/types';
import type { Event } from 'components/events/events/types';

const usePluggableEventActions = (events: Array<Event>, onlyBulk: boolean = false) => {
  const modalRefs = useRef({});
  const pluggableActions = usePluginEntities('views.components.eventActions');
  const availableActions = pluggableActions.filter(
    (perspective) => (onlyBulk ? perspective.isBulk : true) && (perspective.useCondition ? !!perspective.useCondition(events) : true),
  );

  const actions = availableActions.map(({ component: PluggableEventAction, key }: { component: React.ComponentType<EventActionComponentProps>, key: string }) => (
    <PluggableEventAction key={`event-action-${key}`}
                          events={events}
                          modalRef={() => modalRefs.current[key]} />
  ));

  const actionModals = availableActions
    .filter(({ modal }) => !!modal)
    .map(({ modal: ActionModal, key }) => (
      <ActionModal key={`event-action-modal-${key}`}
                   events={events}
                   ref={(r) => { modalRefs.current[key] = r; }} />
    ));

  return ({ actions, actionModals });
};

export default usePluggableEventActions;
