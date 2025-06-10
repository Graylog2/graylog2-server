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
import { configureStore } from '@reduxjs/toolkit';
import type { Reducer, UnknownAction, ReducersMapObject } from 'redux';

export type PluggableReducer<T> = {
  key: keyof T;
  reducer: Reducer<T[keyof T], UnknownAction>;
};

const createStore = <T>(reducers: Array<PluggableReducer<T>>, initialState: Partial<T>, extraArgument?: unknown) => {
  const reducer = Object.fromEntries(reducers.map((r) => [r.key, r.reducer])) as ReducersMapObject<T, UnknownAction>;

  return configureStore({
    reducer,
    preloadedState: initialState,
    middleware: (getDefaultMiddleware) =>
      getDefaultMiddleware({
        serializableCheck: false,
        immutableCheck: false,
        thunk: {
          extraArgument,
        },
      }),
  });
};

export default createStore;
