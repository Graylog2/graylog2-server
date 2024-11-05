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
import React, { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { Event, EventDefinitionContext } from 'components/events/events/types';
import EventDetailsTable from 'components/events/events/EventDetailsTable';
import { detailsAttributes } from 'components/events/Constants';
import MetaDataProvider from 'components/common/EntityDataTable/contexts/MetaDataProvider';
import type { EventsAdditionalData } from 'components/events/fetchEvents';
import EventActions from 'components/events/events/EventActions';

import DropdownButton from '../../bootstrap/DropdownButton';

export const usePluggableEventActions = (eventId: string) => {
  const pluggableEventActions = usePluginEntities('views.components.eventActions');

  return pluggableEventActions.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  ).map(
    ({ component: PluggableEventAction, key }) => (
      <PluggableEventAction key={key} eventId={eventId} />
    ),
  );
};

type Props = {
  event: Event,
  eventDefinitionContext: EventDefinitionContext,
};

const attributesList = detailsAttributes.map(({ id, title }) => ({ id, title }));

const ActionsWrapper = ({ children }) => (
  <DropdownButton title="Actions"
                  buttonTitle="Actions">
    {children}
  </DropdownButton>
);

const EventDetails = ({ event, eventDefinitionContext }: Props) => {
  const meta = useMemo(() => ({ context: { event_definitions: eventDefinitionContext } }), [eventDefinitionContext]);

  return (
    <MetaDataProvider<EventsAdditionalData> meta={meta}>
      <EventDetailsTable attributesList={attributesList} event={event} meta={meta} />
      <EventActions event={event} wrapper={ActionsWrapper} />
    </MetaDataProvider>
  );
};

export default EventDetails;
