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
import React from 'react';

import useCurrentUser from 'hooks/useCurrentUser';
import { isPermitted } from 'util/PermissionsMixin';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { Event, EventDefinitionContext } from 'components/events/events/types';

type Props = {
  event: Event,
  eventDefinitionContext: EventDefinitionContext,
}

const EventDefinitionLink = ({ event, eventDefinitionContext }: Props) => {
  const currentUser = useCurrentUser();

  if (!eventDefinitionContext) {
    return <em>{event.event_definition_id}</em>;
  }

  return isPermitted(currentUser.permissions,
    `eventdefinitions:edit:${eventDefinitionContext.id}`)
    ? <Link to={Routes.ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>{eventDefinitionContext.title}</Link>

    : <>{eventDefinitionContext.title}</>;
};

export default EventDefinitionLink;
