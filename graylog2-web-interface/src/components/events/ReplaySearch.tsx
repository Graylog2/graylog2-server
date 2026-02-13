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

import useCreateSearch from 'views/hooks/useCreateSearch';
import SearchPage from 'views/pages/SearchPage';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';
import { isSystemEventDefinition } from 'components/event-definitions/event-definitions-types';
import Center from 'components/common/Center';
import type View from 'views/logic/views/View';

type ReplaySearchProps = {
  view: Promise<View>;
};

const ReplaySearch = ({ view: _view }: ReplaySearchProps) => {
  const view = useCreateSearch(_view);

  return <SearchPage view={view} isNew />;
};

const canReplayEvent = (eventDefinition: EventDefinition) => {
  const systemEvent = isSystemEventDefinition(eventDefinition);
  if (systemEvent) {
    return 'Event is a system event, these have no query/stream/time range attached.';
  }

  return true;
};

export const LoadingBarrier = ({ children, eventDefinition }) => {
  const canReplay = canReplayEvent(eventDefinition);
  if (canReplay === true) return children;

  return <Center>Cannot replay this event: {canReplay} Please select a different one.</Center>;
};

export default ReplaySearch;
