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

import { isPermitted } from 'util/PermissionsMixin';
import usePluginEntities from 'hooks/usePluginEntities';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'components/events/events/hooks/useEventDefinition';
import { Spinner } from 'components/common';
import DefaultDetails from 'views/components/widgets/events/EventsList/DefaultDetails';
import useCurrentUser from 'hooks/useCurrentUser';

export const usePluggableEventDetails = (eventId: string) => {
  const pluggableEventDetails = usePluginEntities('views.components.widgets.events.detailsComponent');

  return pluggableEventDetails.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  ).map(
    ({ component: PluggableEventAction, key }) => (
      <PluggableEventAction key={key} eventId={eventId} />
    ),
  );
};

export const DefaultDetailsWrapper = ({ eventId }: { eventId: string }) => {
  const { data: event, isLoading } = useEventById(eventId);
  const currentUser = useCurrentUser();
  const canViewDefinition = isPermitted(currentUser.permissions, `eventdefinitions:read:${event?.event_definition_id}`);
  const { data: eventDefinition, isFetching } = useEventDefinition(event?.event_definition_id, canViewDefinition);

  if (isFetching) {
    return <Spinner />;
  }

  if (isLoading) {
    return <Spinner />;
  }

  return <DefaultDetails event={event} eventDefinitionContext={eventDefinition} />;
};

const EventDetailsWrapper = ({ eventId }: { eventId: string }) => {
  const puggableEventDetails = usePluggableEventDetails(eventId);

  if (puggableEventDetails?.length) {
    return <>{puggableEventDetails}</>;
  }

  return <DefaultDetailsWrapper eventId={eventId} />;
};

export default EventDetailsWrapper;
