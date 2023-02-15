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

import { useMemo } from 'react';
import { useRouteMatch } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';

import Routes from 'routing/Routes';
import usePaginationQueryParameter from 'hooks/usePaginationQueryParameter';
import useParams from 'routing/useParams';
import type { EventType } from 'hooks/useEventById';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

const useAlertAndEventDefinitionData = () => {
  const { path } = useRouteMatch();
  const { alertId } = useParams<{ alertId?: string }>();
  const result = usePaginationQueryParameter();
  const queryClient = useQueryClient();
  const eventData = queryClient.getQueryData(['event-by-id', alertId]) as EventType;
  const EDData = queryClient.getQueryData(['definition', eventData?.event_definition_id]) as EventDefinition;

  return useMemo(() => ({
    alertId,
    definitionId: EDData?.id,
    definitionTitle: EDData?.title,
    isAlert: Routes.ALERTS.replay_search(':alertId') && eventData && eventData.alert,
    isEvent: Routes.ALERTS.replay_search(':eventId') && eventData && !eventData.alert,
    isEventDefinition: Routes.ALERTS.DEFINITIONS.replay_search(':definitionId') && EDData,
    eventData,
    EDData,
  }), [path]);
};

export default useAlertAndEventDefinitionData;
