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
export type Subscribable<S> = {
  getState: () => S;
  subscribe: (listener: () => void) => () => void;
};

export type ExternalStore<S> = Subscribable<S> & {
  setState: (next: Partial<S>) => void;
};

const createExternalStore = <S extends object>(initialState: S): ExternalStore<S> => {
  let state = initialState;
  const listeners = new Set<() => void>();

  return {
    getState: () => state,
    setState: (next: Partial<S>) => {
      if (Object.entries(next).every(([key, value]) => Object.is(state[key], value))) {
        return;
      }

      state = { ...state, ...next };
      listeners.forEach((listener) => listener());
    },
    subscribe: (listener: () => void) => {
      listeners.add(listener);

      return () => {
        listeners.delete(listener);
      };
    },
  };
};

export default createExternalStore;
