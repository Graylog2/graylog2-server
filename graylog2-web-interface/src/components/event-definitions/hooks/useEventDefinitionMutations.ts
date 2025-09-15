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
import { useMutation } from '@tanstack/react-query';
import pick from 'lodash/pick';
import cloneDeep from 'lodash/cloneDeep';
import omit from 'lodash/omit';

import { EventsDefinitions } from '@graylog/server-api';

import UserNotification from 'util/UserNotification';
import type { EntityShare } from 'actions/permissions/EntityShareActions';

import type { EventDefinition } from '../event-definitions-types';

const extractSchedulerInfo = (eventDefinition: EventDefinition) => {
  // Removes the internal "_is_scheduled" field from the event definition data. We only use this to pass-through
  // the flag from the form.
  const clonedEventDefinition = cloneDeep(eventDefinition);
  const { _is_scheduled } = pick(clonedEventDefinition.config, ['_is_scheduled']);

  clonedEventDefinition.config = omit(clonedEventDefinition.config, ['_is_scheduled']);

  return { eventDefinition: clonedEventDefinition, isScheduled: _is_scheduled ?? true };
};

const createEventDefinition = async (newEventDefinition: EventDefinition & EntityShare) => {
  const { share_request, ...rest } = newEventDefinition;
  const { eventDefinition, isScheduled } = extractSchedulerInfo(rest);

  return EventsDefinitions.create(isScheduled, {
    entity: eventDefinition,
    share_request: {
      selected_collections: share_request?.selected_collections,
      selected_grantee_capabilities: share_request?.selected_grantee_capabilities?.toJS(),
    },
  });
};

const useEventDefinitionMutations = () => {
  const createMutation = useMutation({
    // @ts-ignore
    mutationFn: createEventDefinition,
    onError: (errorThrown) => {
      UserNotification.error(
        `Saving EventDefinition failed with status: ${errorThrown}`,
        'Could not save EventDefinition',
      );
    },
    onSuccess: (eventDefinition: EventDefinition) => {
      UserNotification.success(
        'Event Definition created successfully',
        `Event Definition "${eventDefinition.title}" was created successfully.`,
      );
    },
  });

  return { createEventDefinition: createMutation.mutateAsync };
};

export default useEventDefinitionMutations;
