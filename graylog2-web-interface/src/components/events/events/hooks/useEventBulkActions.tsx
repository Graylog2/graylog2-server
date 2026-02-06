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
import isEmpty from 'lodash/isEmpty';

import usePluggableEventActions from 'components/events/events/hooks/usePluggableEventActions';
import type { Event } from 'components/events/events/types';

const useEventBulkAction = (events: Array<Event>, onEventCallback?: () => void) => {
  const { actions: pluggableActions, actionModals: pluggableActionModals } = usePluggableEventActions(
    events,
    true,
    onEventCallback,
  );

  const actions = useMemo(() => pluggableActions.filter(Boolean), [pluggableActions]);

  if (isEmpty(actions)) return { actions: null, pluggableActionModals: null };

  if (isEmpty(flatten(actions))) return { actions: null, pluggableActionModals: null };

  return { actions, pluggableActionModals };
};

export default useEventBulkAction;
