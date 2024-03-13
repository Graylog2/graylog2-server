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
import { useQuery } from '@tanstack/react-query';

import type { EventDefinition } from 'logic/alerts/types';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'preflight/util/UserNotification';
import { Spinner } from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

export const fetchEventDefinitionDetails = async (eventDefinitionId: string): Promise<EventDefinition> => (
  fetch('GET', qualifyUrl(`/events/definitions/${eventDefinitionId}`))
);

const useEventDefinitionDetails = (eventDefId: string) => {
  const { data, isFetching } = useQuery({
    queryKey: ['get-event-definition-details', eventDefId],
    queryFn: () => fetchEventDefinitionDetails(eventDefId),
    onError: (errorThrown) => {
      UserNotification.error(`Loading archives failed with status: ${errorThrown}`);
    },
    retry: 0,
    keepPreviousData: true,
  });

  return { data, isFetching };
};

type Props = {
  eventDefinitionId: string,
  displayAsLink?: boolean,
}

const EventDefinitionName = ({ eventDefinitionId, displayAsLink }: Props) => {
  const { data: eventDefinition, isFetching } = useEventDefinitionDetails(eventDefinitionId);

  if (isFetching) {
    return <Spinner />;
  }

  if (!displayAsLink) {
    return <>{eventDefinition.title}</>;
  }

  if (eventDefinition) {
    return (
      <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)} target="_blank">
        {eventDefinition.title}
      </Link>
    );
  }

  return <>eventDefinitionId</>;
};

EventDefinitionName.defaultProps = {
  displayAsLink: true,
};

export default EventDefinitionName;
