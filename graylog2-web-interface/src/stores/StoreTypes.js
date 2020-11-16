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
// @flow strict

type ExtractResultType = <R>((...any) => Promise<R>) => R;

export type ListenableAction<R> = R & {
  $call: R,
  listen: (($Call<ExtractResultType, R>) => *) => () => void,
  completed: {
    listen: (($Call<ExtractResultType, R>) => *) => () => void,
  },
  promise: (Promise<$Call<ExtractResultType, R>>) => void,
};
export type RefluxAction = <Fn>(Fn) => ListenableAction<Fn>;

export type RefluxActions<A> = $ObjMap<A, RefluxAction>;

export type Store<State> = {
  listen: ((State) => mixed) => () => void,
  getInitialState: () => State,
};
