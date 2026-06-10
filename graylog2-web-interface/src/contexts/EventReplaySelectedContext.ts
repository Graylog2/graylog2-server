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

import { singleton } from 'logic/singleton';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';
import type { Event } from 'components/events/events/types';

export type Warnings = {
  queryString?: QueryValidationState;
};

type SelectCallback = (eventId: string) => void;

export type ResolutionState = { id: string; status: 'OPEN' | 'DONE' };

export type SelectedState = {
  selectedId: string;
  eventIds: Array<ResolutionState>;
};

export type SelectedEventsData = { [eventId: string]: { event: Event } };

type EventReplaySelectedContextType = SelectedState & {
  removeItem: SelectCallback;
  markItemAsDone: SelectCallback;
  selectItem: SelectCallback;
  eventsData: SelectedEventsData;
};

const EventReplaySelectedContext = React.createContext<EventReplaySelectedContextType | undefined>(undefined);

export default singleton('contexts.EventReplaySelectedContext', () => EventReplaySelectedContext);
