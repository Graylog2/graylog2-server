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

import React, { useEffect, useState } from 'react';

import useParams from 'routing/useParams';
import useEventById from 'hooks/useEventById';
import useEventDefinition from 'hooks/useEventDefinition';
import { Spinner } from 'components/common';
import SearchPage from 'views/pages/SearchPage';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import useCreateViewForEvent from 'views/logic/views/UseCreateViewForEvent';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import EventInfoBar from 'components/event-definitions/replay-search/EventInfoBar';

const EventView = () => {
  const { eventData, eventDefinition, aggregations } = useAlertAndEventDefinitionData();
  const view = useCreateViewForEvent({ eventData, eventDefinition, aggregations });

  return (
    <SearchPage view={view}
                isNew
                SearchComponentSlots={{
                  InfoBarSlot: EventInfoBar,
                }} />
  );
};

const EventReplaySearchPage = () => {
  const [isNotificationLoaded, setIsNotificationLoaded] = useState(false);
  const { alertId } = useParams<{ alertId?: string }>();
  const { data: eventData, isLoading: eventIsLoading, isFetched: eventIsFetched } = useEventById(alertId);
  const { isLoading: EDIsLoading, isFetched: EDIsFetched } = useEventDefinition(eventData?.event_definition_id);

  useEffect(() => {
    EventNotificationsActions.listAll().then(() => setIsNotificationLoaded(true));
  }, [setIsNotificationLoaded]);

  const isLoading = eventIsLoading || EDIsLoading || !eventIsFetched || !EDIsFetched || !isNotificationLoaded;

  return isLoading ? <Spinner /> : <EventView />;
};

export default EventReplaySearchPage;
