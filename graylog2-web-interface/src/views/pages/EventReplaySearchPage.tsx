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
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';
import ReplaySearch from 'components/events/ReplaySearch';

export const onErrorHandler = (error) => {
  if (error.status === 404) {
    ErrorsActions.report(createFromFetchError(error));
  }
};

const EventReplaySearchPage = () => {
  const [isNotificationLoaded, setIsNotificationLoaded] = useState(false);
  const { alertId, definitionId } = useParams<{ alertId?: string, definitionId?: string }>();
  const { data: eventData, isLoading: eventIsLoading, isFetched: eventIsFetched } = useEventById(alertId, { onErrorHandler });
  const { isLoading: EDIsLoading, isFetched: EDIsFetched } = useEventDefinition(eventData?.event_definition_id);

  useEffect(() => {
    EventNotificationsActions.listAll().then(() => setIsNotificationLoaded(true));
  }, [setIsNotificationLoaded]);

  const isLoading = eventIsLoading || EDIsLoading || !eventIsFetched || !EDIsFetched || !isNotificationLoaded;

  return isLoading
    ? <Spinner />
    : <ReplaySearch alertId={alertId} definitionId={definitionId} />;
};

export default EventReplaySearchPage;
