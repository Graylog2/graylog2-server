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
import useEventDefinition from 'hooks/useEventDefinition';
import { Spinner } from 'components/common';
import { EventNotificationsActions } from 'stores/event-notifications/EventNotificationsStore';
import { createFromFetchError } from 'logic/errors/ReportedErrors';
import ErrorsActions from 'actions/errors/ErrorsActions';
import EventDefinitionReplaySearch from 'components/event-definitions/replay-search/EventDefinitionReplaySearch';
import type FetchError from 'logic/errors/FetchError';

export const onErrorHandler = (error: FetchError) => {
  if (error.status === 404) {
    ErrorsActions.report(createFromFetchError(error));
  }
};

const EventDefinitionReplaySearchPage = () => {
  const [isNotificationLoaded, setIsNotificationLoaded] = useState(false);
  const { definitionId } = useParams<{ alertId?: string; definitionId?: string }>();
  const { isLoading: EDIsLoading, isFetched: EDIsFetched, data } = useEventDefinition(definitionId, { onErrorHandler });

  useEffect(() => {
    EventNotificationsActions.listAll().then(() => setIsNotificationLoaded(true));
  }, [setIsNotificationLoaded]);

  const isLoading = EDIsLoading || !EDIsFetched || !isNotificationLoaded;

  return isLoading ? <Spinner /> : <EventDefinitionReplaySearch eventDefinitionMappedData={data} />;
};

export default EventDefinitionReplaySearchPage;
