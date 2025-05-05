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

import { useMutation, useQueryClient } from '@tanstack/react-query';

import { EventsDefinitions } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

const createEventDefinitionWithShare = async ({
  eventDefinition,
  shareRequest,
}: {
  eventDefinition: EventDefinition;
  shareRequest: EntitySharePayload;
}) =>
  EventsDefinitions.createWithRequest(true, {
    entity: eventDefinition as any,
    share_request: { selected_grantee_capabilities: shareRequest.selected_grantee_capabilities.toJS() },
  });

const useEventDefinitionWithShareMutation = () => {
  const queryClient = useQueryClient();
  const addMutation = useMutation(createEventDefinitionWithShare, {
    onError: (errorThrown) => {
      UserNotification.error(
        `Creating event definition failed with status: ${errorThrown}`,
        'Could not create event definition',
      );
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['eventDefinition', 'overview']);
      UserNotification.success('Even Definition has been successfully created.', 'Success!');
    },
  });

  return { createEventDefinitionWithShare: addMutation.mutateAsync };
};

export default useEventDefinitionWithShareMutation;
