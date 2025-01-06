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
import { useCallback, useReducer } from 'react';

import assertUnreachable from 'logic/assertUnreachable';

type ResolutionState = { id: string, status: 'OPEN' | 'DONE' };

type Action = {
  type: 'remove' | 'done' | 'select',
  id: string,
}
type State = {
  selectedId: string,
  eventIds: Array<ResolutionState>,
}

const pickNextId = (eventIds: Array<ResolutionState>) => eventIds.find((event) => event.status === 'OPEN')?.id;

const createInitialState = (_eventIds: Array<string>) => {
  const eventIds = _eventIds.map((id) => ({ id, status: 'OPEN' } as const));
  const selectedId = pickNextId(eventIds);

  return {
    selectedId,
    eventIds,
  };
};

const reducer = (state: State, action: Action) => {
  if (action.type === 'remove') {
    const eventIds = state.eventIds.filter((event) => event.id !== action.id);
    const selectedId = state.selectedId === action.id
      ? pickNextId(eventIds)
      : state.selectedId;

    return {
      selectedId,
      eventIds,
    };
  }

  if (action.type === 'done') {
    const eventIds = state.eventIds.map((event) => (event.id === action.id
      ? { id: action.id, status: 'DONE' } as const
      : event));
    const selectedId = state.selectedId === action.id
      ? pickNextId(eventIds)
      : state.selectedId;

    return {
      selectedId,
      eventIds,
    };
  }

  if (action.type === 'select') {
    return {
      ...state,
      selectedId: action.id,
    };
  }

  return assertUnreachable(action.type, `Invalid action dispatched: ${action}`);
};

const useSelectedEvents = (eventIds: Array<string>) => {
  const [state, dispatch] = useReducer(reducer, createInitialState(eventIds));
  const removeItem = useCallback((eventId: string) => dispatch({ type: 'remove', id: eventId }), []);
  const markItemAsDone = useCallback((eventId: string) => dispatch({ type: 'done', id: eventId }), []);
  const selectItem = useCallback((eventId: string) => dispatch({ type: 'select', id: eventId }), []);

  return {
    ...state,
    removeItem,
    markItemAsDone,
    selectItem,
  };
};

export default useSelectedEvents;
